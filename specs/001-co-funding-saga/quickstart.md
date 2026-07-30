# Quickstart 검증 가이드: 공동 결제(더치페이) 설문 개설

**Feature**: 001-co-funding-saga | 계약: [contracts/](contracts/) | 모델: [data-model.md](data-model.md)

## 전제

- 로컬 인프라 기동: `docker-compose up -d` (MySQL·Redis·Kafka·모니터링)
- web 기동: `./gradlew :web:bootRun` (토스 테스트 키를 `application-secret.yml`에 설정)
- consumer 기동: `module-consumer/co-funding-consumer`에서 `bootJar` 후 실행
  (집계·환불 리스너, 기한 스케줄러 포함 빌드)
- 부하 테스트 JWT: 기존 부하 테스트용 JWT 발급 엔드포인트로 참여자 계정 토큰 확보

## 시나리오 S1 — 전원 결제 → 자동 개설 (US1, SC-001)

1. 개설자로 설문 생성 + 모금 개시(capacity=3, 기한 1일) → 응답의 `inviteUrl` 확인
2. 참여자 2명 등록(계약 §2) → 각자 주문 발급(§4) → 브라우저 checkout에서 토스
   테스트 카드로 결제 → 개설자도 동일하게 결제
3. **기대**: 마지막 결제 확정 후 1분 내 —
   - `co_fundings.status = CONFIRMED`, Survey `IN_PROGRESS`
   - 현황 API(§5) `settledCount=3`, `status=CONFIRMED`
   - 설문 응답 제출 가능

## 시나리오 S2 — 기한 만료 → 부채꼴 전액 환불 (US2, SC-002)

1. capacity=3 모금 개시, 참여자 2명만 결제(1명 미결제), 기한을 짧게(또는 DB에서
   deadline을 과거로 조정)
2. consumer의 기한 스케줄러 주기 대기
3. **기대**: 10분 내 —
   - `co_fundings`: FUNDING → FAILED(DEADLINE_EXPIRED) → REFUNDED
   - 결제된 주문 2건 모두 `payment_orders.status = CANCELED`, CANCEL 커맨드 2건
     CONFIRMED(토스 조회로 실취소 확인)
   - 환불 누락 0·이중 환불 0 (`payment_commands`에서 orderId당 CANCEL 1건)

## 시나리오 S3 — 경합 (FR-005·FR-009, SC-003)

- **S3a 동시 결제**: k6로 참여자 N명의 confirm을 동시 발사 → CONFIRMED 전이
  정확히 1회(전이 카운터 메트릭 = 1), `co-funding-completed` 발행 1건
- **S3b 마지막 결제 vs 기한 만료**: deadline 직전 결제와 스케줄러 발화를 겹침 →
  최종 상태가 CONFIRMED **또는** REFUNDED 중 정확히 하나. CONFIRMED면 환불 0건,
  FAILED면 늦은 확정 주문까지 전액 환불(D6)
- **S3c 마지막 자리 동시 등록**: 남은 자리 1에 등록 요청 동시 다발 →
  `registered_count == capacity` 유지, 초과 등록 0

## 시나리오 S4 — 중단·재개 수렴 (FR-013, SC-004)

1. S2를 진행하되 환불 커맨드 일부만 적재된 시점(또는 릴레이 발송 직전)에 web
   프로세스 강제 종료
2. 재기동
3. **기대**: 릴레이·리스너가 이어받아 전원 CANCELED + REFUNDED 도달. 수동 개입 0

## 검증 실행 위치

- k6 스크립트: `scripts/scenarios/saga/co-funding-*.js` (S1~S4 자동화분)
- 러너: `scripts/runners/` 에 공동 모금 러너 추가, 레포 루트에서 실행
- 단위 테스트: `./gradlew test` (분담금 산정·상태 전이·CAS 판정)
- 품질 게이트: `./gradlew build ktlintCheck` 상시 통과 (헌장 III)

## 관측 확인 (D9)

- Grafana(`localhost:13000`) co-funding 대시보드 — 전이 카운터, 환불 수렴 시간
  히스토그램, 미수렴 CANCEL gauge
- Tempo에서 confirm→settled→집계→활성화 trace가 Kafka 경계를 넘어 이어지는지 확인
