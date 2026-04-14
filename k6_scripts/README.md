# Lost Update 검증 k6 테스트

## 목적

`@RedissonLock`과 `@Transactional`의 실행 순서가 보장되지 않을 때 발생하는 **Lost Update** 문제를 실제로 검증합니다.

## 사전 준비

### 1. k6 설치

```bash
brew install k6
```

### 2. 서버 실행

로컬에서 Spring Boot 서버가 `http://localhost:8080`에서 실행 중이어야 합니다.

### 3. 테스트 데이터 준비

DB에 아래 데이터가 필요합니다:

- **설문(Survey)**: `status = IN_PROGRESS`, 추첨 기능 활성화
- **추첨보드(DrawingBoard)**: 해당 설문에 연결, 티켓 20장 (당첨 + 꽝)
- **참가자(Participant)**: 20명 (설문에 응답 완료 상태)

## 테스트 실행

### 테스트 1: 서로 다른 사용자 × 서로 다른 티켓

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e SURVEY_ID=실제-설문-UUID \
  -e PARTICIPANT_IDS='["uuid1","uuid2",...,"uuid20"]' \
  k6_scripts/lost-update-test.js
```

**검증**: 20명 모두 성공해야 정상. API 성공 수 != DB selectedTicketCount면 Lost Update.

### 테스트 2: 같은 티켓 경합 (Race Condition)

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e SURVEY_ID=실제-설문-UUID \
  -e PARTICIPANT_IDS='["uuid1","uuid2",...,"uuid10"]' \
  k6_scripts/same-ticket-race-test.js
```

**검증**: 같은 티켓에 1명만 성공해야 정상. 2명 이상 성공하면 Lost Update.

## Lost Update 시나리오

```
시간 →

Thread A: Lock 획득 → Read(ticket=미선택) → Write(ticket=선택) → Lock 해제 → TX 커밋
Thread B:              Lock 대기...        Lock 획득 →              Read(미선택!) → Lost Update!
                                                     ↑ TX 커밋 전이라 아직 DB에 반영 안 됨
```

### 현재 구현이 이를 방지하는 방법

`RedissonLockAspect`가 `TransactionSynchronization.afterCompletion()`을 사용:

```
Thread A: Lock 획득 → Read → Write → TX 커밋 → Lock 해제
Thread B:            Lock 대기...               Lock 획득 → Read(선택됨!) → 정상 거부
```

## 면접 포인트

**Q**: `@RedissonLock`과 `@Transactional` 순서가 보장되지 않으면 어떤 문제가 생기나?

**A**: 트랜잭션 커밋 전에 락이 해제되어 다른 스레드가 커밋되지 않은(stale) 데이터를 기반으로 작업할 수 있다. 현재 구현은 `TransactionSynchronization.afterCompletion()`으로 트랜잭션 커밋/롤백 완료 후에만 unlock하여 이 문제를 방지한다.

**Q**: `@Order(Ordered.LOWEST_PRECEDENCE - 1)` 방식과 현재 `TransactionSynchronization` 방식의 차이는?

**A**: `@Order` 방식은 AOP 프록시 실행 순서를 제어하여 Lock AOP가 Transaction AOP를 감싸도록 한다. 현재 방식은 AOP 순서와 무관하게, 트랜잭션이 활성화되어 있으면 커밋 콜백에 unlock을 등록한다. 현재 방식이 더 안전 — AOP 순서에 의존하지 않는다.
