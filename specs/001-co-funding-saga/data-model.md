# Data Model: 공동 결제(더치페이) 설문 개설

**Feature**: 001-co-funding-saga | **Date**: 2026-07-26 | 근거: [research.md](research.md) D1·D2·D4·D7

## 신규 테이블

### co_fundings (공동 모금) — Flyway V9

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BINARY(16) | PK | UUID (기존 Participant.kt 매핑 관례) |
| survey_id | BINARY(16) | UNIQUE, NOT NULL | 설문과 1:1 |
| owner_id | BINARY(16) | NOT NULL | 개설자 (개설 시 참여자 명단에 포함) |
| capacity | INT | NOT NULL, >= 2 | 공동 주최 인원 = 명단 크기(개설자 포함), 개설 후 불변 |
| registered_count | INT | NOT NULL, DEFAULT 0 | 미사용 — 초대제 전환(D7)으로 잔존, 엔티티 미매핑 |
| share_amount | INT | NOT NULL | 참여자 분담금(원). 잔액은 개설자 분담금에 합산 → owner_share_amount |
| owner_share_amount | INT | NOT NULL | 개설자 분담금 = share_amount + 잔액 |
| deadline | DATETIME | NOT NULL | 모금 기한 (개설 시점 + 최대 7일) |
| status | VARCHAR(20) | NOT NULL | 아래 상태 머신 |
| fail_reason | VARCHAR(30) | NULL | 미사용 — 무산 트리거가 기한 만료 하나라 사유 기록 불필요, 컬럼 잔존·엔티티 미매핑 |
| created_at / updated_at | DATETIME | NOT NULL | 공통 감사 필드 |

인덱스: `INDEX(status, deadline)` — 기한 스케줄러 SKIP LOCKED 스캔용(D8).

**상태 머신 (조건부 전이만 허용, D4)**

```
FUNDING ──(확정 수 = capacity, CAS)──▶ CONFIRMED        [설문 활성화]
FUNDING ──(기한 만료, CAS)──────────▶ FAILED            [failed 발행 → 환불 팬아웃]
FAILED  ──(전원 환불 완료 판정)──────▶ REFUNDED
```

- `FUNDING → CONFIRMED`와 `FUNDING → FAILED`는 상호 배타(FR-009) — 같은 행 CAS라
  DB가 한쪽만 승자로 만든다.
- REFUNDED 판정: 결제 확정 참여자 전원의 주문이 CANCELED가 된 시점 — 릴레이의
  CANCEL 후처리가 전이 tx(참여자 REFUNDED) 커밋 후 별도 판정 tx에서 조건부
  전이를 시도한다(D5, 잠금 교차 제거). 컨슈머 환불 리스너는 CANCEL 적재만 담당.

### co_funding_participants (모금 참여자)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BINARY(16) | PK | UUID |
| funding_id | BINARY(16) | NOT NULL, FK 개념 | co_fundings 참조 |
| user_id | BINARY(16) | NOT NULL | 참여 메이커 |
| role | VARCHAR(10) | NOT NULL | OWNER / MEMBER |
| status | VARCHAR(20) | NOT NULL | 아래 상태 머신 |
| order_id | VARCHAR(64) | NULL, UNIQUE | 개설 트랜잭션에서 사전 발급되는 주문 ID (payment_orders 연결, D7) |
| settled_at | DATETIME | NULL | 결제 확정 시각 |
| created_at / updated_at | DATETIME | NOT NULL | |

제약: `UNIQUE(funding_id, user_id)` — 명단 내 중복 계정 차단(FR-004) + 장벽
카운트의 근거 행(D4: 판정은 `status='SETTLED'` 행 수를 센다).

**상태 머신**

```
REGISTERED ──(결제 confirm 확정, 모금 FUNDING일 때)──▶ SETTLED
SETTLED    ──(무산 → CANCEL 커맨드 수렴)──▶ REFUNDED
```

- REGISTERED는 초대 확정·미결제 상태 — 명단은 개설 시점에 닫히므로(초대제, D7)
  개별 이탈 경로는 없고, 무산(기한 만료) 시 전원 환불만 존재한다.
- REGISTERED 상태로 기한 도달 → 모금 무산의 원인이 되고 행은 그대로 종료 상태 유지.

## 기존 테이블 변경 — Flyway V9

### payment_orders (D1)

- `UNIQUE(survey_id)` 제거 → `INDEX(survey_id)`로 완화. 공동 모금은 설문 하나에
  참여자별 주문 N건.
- 컬럼 추가 없음 — 주문의 모금 귀속은 `co_funding_participants.order_id`가 가리킨다
  (주문 테이블은 결제 장부 역할만 유지, 도메인 역참조 없음).

### payment_commands (D5)

- `command_type`에 `CANCEL` 값 추가 (enum 문자열 — DDL 변경 없음, 코드만).
- `UNIQUE(aggregate_id, command_type)` 제약 추가 — 주문당 CANCEL 1회 멱등.
  (기존 CONFIRM 흐름도 주문당 1회이므로 소급 위반 없음 — V9에서 제약 추가 전
  중복 검사 필요.)

## 엔티티 배치 (헌장 I)

| 코드 | 모듈 |
|---|---|
| `CoFunding`, `CoFundingParticipant` 엔티티 + 리포지토리 | `:support` `cofunding/entity·repository` |
| 이벤트 DTO (`CoPaymentSettledEvent` 등) | `:support` `cofunding/dto/event` (wire 계약, D11) |
| 모금 서비스·publisher·정산 확장·CANCEL 릴레이 확장 | `:produce` |
| 컨트롤러(등록·취소·현황·철회) | `:web` |
| 집계·환불 리스너, 기한 스케줄러, JPA 엔티티·리포지토리 사본(D12) | `module-consumer/co-funding-consumer` 신규 모듈 (DTO 사본은 `module-consumer/common`) |

## 불변식 요약

1. 모금:설문 = 1:1 (`co_fundings.survey_id UNIQUE`)
2. 모금 내 참여자 유일 (`UNIQUE(funding_id, user_id)`)
3. 참여자 행 수 = capacity (개설 트랜잭션에서 명단 일괄 확정, D7 — 이후 불변)
4. CONFIRMED와 FAILED는 공존 불가 (단일 행 CAS)
5. SETTLED 행 수 = capacity ⇔ CONFIRMED 전이 가능 (장벽 판정)
6. 주문당 CANCEL 커맨드 최대 1건 (`UNIQUE(aggregate_id, command_type)`)
7. 무산 확정 후 신규 SETTLED 전이 불가 — settle 트랜잭션이 모금 상태를 검사(D6),
   FAILED면 즉시 CANCEL 적재
