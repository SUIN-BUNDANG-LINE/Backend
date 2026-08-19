#!/usr/bin/env bash
# 공동 모금 사가 종단 검증 — 주입(DB·Kafka) + 관찰(DB 폴링) 방식.
#
# 토스 결제창이 필요한 confirm 경로는 자동화가 불가능하므로, 결제의 "사실"(④ payment-succeeded)을
# 주입해 그 이후의 사가(모금 리스너 SETTLED 전이 → 장벽 → ⑤ → 설문 활성화, 무산 → ⑥ 팬아웃 → 멱등)를
# 실증한다. 구조변경(단일 기록자) 신 배선 기준 — SETTLED 전이도 모금 ④ 리스너가 수행한다.
#
# 선행 조건:
#   - MySQL: sulmun2yong-cluster-mysql 컨테이너 기동 (서비스별 스키마 - survey_db·cofunding_db·payment_db)
#   - Kafka: kafka-b1.q-asker.com 브로커 접근 가능 (kcat 도커 이미지 사용)
#   - 3-JVM 기동: module-cofunding(8083, ④⑦리스너·기한 스케줄러) · module-payment(8082, ⑥⑧리스너) ·
#     web(설문 ⑤·단독 리스너). 예: java -jar 각 bootJar
#
# 실행: bash scripts/scenarios/saga/co-funding-saga-verify.sh
#
# 시나리오:
#   S1 장벽·활성화·멱등 — 1/2 결제(no-op) → 2/2 결제(승자·활성화) → 중복 이벤트(no-op)
#   S2 만료·팬아웃·이중적재 방어 — 스케줄러 무산 → CANCEL N건 적재 → failed 재발행에도 추가 0건
#   S3 경합 상호배타 — 만료 직후 결제 확정 이벤트: 개설 xor 무산 정확히 하나만 성립

set -uo pipefail

MYSQL="docker exec sulmun2yong-cluster-mysql mysql -uuser -ppassword -N -s -e"
BROKER="kafka-b1.q-asker.com:9092"
PASS=0
FAIL=0

sql() { eval "$MYSQL \"\$1\"" 2>/dev/null; }

publish() { # topic key json
  echo "$3" | docker run --rm -i edenhill/kcat:1.7.1 -b "$BROKER" -t "$1" -k "$2" -P 2>/dev/null
}

assert_eq() { # label expected actual
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); echo "  ✅ $1 (= $2)"
  else
    FAIL=$((FAIL + 1)); echo "  ❌ $1 — 기대 [$2], 실제 [$3]"
  fi
}

poll() { # sql expected timeout_sec label
  local deadline=$((SECONDS + $3))
  local actual=""
  while [ $SECONDS -lt $deadline ]; do
    actual=$(sql "$1")
    [ "$actual" = "$2" ] && break
    sleep 2
  done
  assert_eq "$4" "$2" "$actual"
}

succeeded_event() { # orderId productId  — ④ payment-succeeded (발행자=결제, key=orderId, 좌표=(productType, productId))
  printf '{"eventId":"%s","orderId":"%s","productType":"DRAWING_BOARD","productId":"%s","succeededAt":"%s"}' \
    "$(uuidgen | tr 'A-Z' 'a-z')" "$1" "$2" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}

failed_event() { # fundingId surveyId orderIdsJson
  printf '{"eventId":"%s","fundingId":"%s","surveyId":"%s","paidOrderIds":%s,"expiredAt":"%s"}' \
    "$(uuidgen | tr 'A-Z' 'a-z')" "$1" "$2" "$3" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}

# 검증용 행 주입 도우미 — 접두사(p)로 시나리오별 UUID 를 격리한다
seed_survey() { # p
  # IMMEDIATE_DRAW 는 경품·정원·마감일 셋이 모두 있어야 유효하다(RewardSetting.of) —
  # 하나라도 빠지면 활성화 시 InvalidRewardSettingException 이라 rewards 행까지 함께 심는다.
  # 마감일은 정시여야 한다(FinishedAt: 분·초·밀리초 0) — 아니면 InvalidFinishedAtException.
  sql "INSERT INTO survey_db.surveys (id, title, description, status, finish_message, reward_setting_type, target_participant_count, finished_at, is_visible, maker_id, is_result_open, is_deleted, created_at, updated_at)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-000000000002'), '사가 검증', '', 'NOT_STARTED', '', 'IMMEDIATE_DRAW', 10, DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 7 DAY), '%Y-%m-%d %H:00:00'), 0, UUID_TO_BIN('$1-0000-0000-0000-000000000003'), 0, 0, NOW(), NOW())"
  sql "INSERT INTO survey_db.rewards (survey_id, order_index, name, category, count)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-000000000002'), 0, '검증용 경품', '기프티콘', 3)"
  # IN_PROGRESS 전이는 섹션이 비어 있으면 InvalidSurveyException — 최소 1개를 심는다.
  # NUMERICAL_ORDER 는 next_section_id·key_question_id 가 필요 없어 가장 단순하다.
  sql "INSERT INTO survey_db.sections (id, survey_id, order_index, title, description, route_type)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-000000000004'), UUID_TO_BIN('$1-0000-0000-0000-000000000002'), 0, '검증용 섹션', '', 'NUMERICAL_ORDER')"
  # 결제 대기 보드 - 활성화 리스너의 가드(결제 대기 보드 존재 = 결제 흐름 증명)가 요구한다.
  sql "INSERT INTO survey_db.drawing_boards (id, survey_id, status, created_at, updated_at)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-000000000005'), UUID_TO_BIN('$1-0000-0000-0000-000000000002'), 'PENDING_PAYMENT', NOW(), NOW())"
}

seed_funding() { # p deadline_expr
  sql "INSERT INTO cofunding_db.co_fundings (id, survey_id, owner_id, capacity, share_amount, deadline, status, created_at, updated_at)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-000000000001'), UUID_TO_BIN('$1-0000-0000-0000-000000000002'), UUID_TO_BIN('$1-0000-0000-0000-000000000003'), 2, 6000, $2, 'FUNDING', NOW(), NOW())"
}

seed_participant() { # p n status
  sql "INSERT INTO cofunding_db.co_funding_participants (id, funding_id, user_id, status, order_id, created_at, updated_at)
       VALUES (UUID_TO_BIN('$1-0000-0000-0000-00000000001$2'), UUID_TO_BIN('$1-0000-0000-0000-000000000001'), UUID_TO_BIN('$1-0000-0000-0000-00000000002$2'), '$3', 'ord-verify-$1-$2', NOW(), NOW())"
}

seed_order() { # p n
  sql "INSERT INTO payment_db.toss_orders (id, product_type, product_id, payer_id, amount, payment_key, status, created_at, updated_at)
       VALUES ('ord-verify-$1-$2', 'DRAWING_BOARD', UUID_TO_BIN('$1-0000-0000-0000-000000000005'), UUID_TO_BIN('$1-0000-0000-0000-00000000002$2'), 6000, 'fake-key-$1-$2', 'SUCCEEDED', NOW(), NOW())"
}

cleanup() { # p
  sql "DELETE FROM payment_db.toss_api_call_outbox WHERE order_id LIKE 'ord-verify-$1-%'"
  sql "DELETE FROM payment_db.toss_orders WHERE id LIKE 'ord-verify-$1-%'"
  sql "DELETE FROM cofunding_db.co_funding_participants WHERE funding_id = UUID_TO_BIN('$1-0000-0000-0000-000000000001')"
  sql "DELETE FROM cofunding_db.co_fundings WHERE id = UUID_TO_BIN('$1-0000-0000-0000-000000000001')"
  sql "DELETE FROM survey_db.drawing_boards WHERE survey_id = UUID_TO_BIN('$1-0000-0000-0000-000000000002')"
  sql "DELETE FROM survey_db.surveys WHERE id = UUID_TO_BIN('$1-0000-0000-0000-000000000002')"
}

FUNDING_STATUS="SELECT status FROM cofunding_db.co_fundings WHERE id = UUID_TO_BIN('%s-0000-0000-0000-000000000001')"
SURVEY_STATUS="SELECT status FROM survey_db.surveys WHERE id = UUID_TO_BIN('%s-0000-0000-0000-000000000002')"
CANCEL_COUNT="SELECT COUNT(*) FROM payment_db.toss_api_call_outbox WHERE call_type='CANCEL' AND order_id LIKE 'ord-verify-%s-%%'"

# ────────────────────────────────────────────────────────────────────────────
echo "═══ S1. 장벽 판정 · 설문 활성화 · 재수신 멱등 ═══"
P=bbbbbbbb
cleanup $P
seed_survey $P
seed_funding $P "NOW() + INTERVAL 1 DAY"
seed_participant $P 1 REGISTERED   # 신 배선: SETTLED 전이는 모금 ④ 리스너가 수행한다
seed_participant $P 2 REGISTERED

FID="$P-0000-0000-0000-000000000001"; SID="$P-0000-0000-0000-000000000002"; BID="$P-0000-0000-0000-000000000005"

echo "· 1/2 결제(④ 발행) → 리스너가 p1 SETTLED 전이, 장벽 미달로 FUNDING 유지"
publish payment-succeeded "ord-verify-$P-1" "$(succeeded_event "ord-verify-$P-1" "$BID")"
poll "SELECT status FROM cofunding_db.co_funding_participants WHERE order_id='ord-verify-$P-1'" "SETTLED" 15 "p1 SETTLED 전이(리스너)"
assert_eq "모금 상태 유지(FUNDING)" "FUNDING" "$(sql "$(printf "$FUNDING_STATUS" $P)")"
assert_eq "설문 미활성(NOT_STARTED - 결제 대기는 보드 몫)" "NOT_STARTED" "$(sql "$(printf "$SURVEY_STATUS" $P)")"

echo "· 2/2 결제(④ 발행) → 장벽 성립(⑤ 발행 승자) → 설문 활성화"
publish payment-succeeded "ord-verify-$P-2" "$(succeeded_event "ord-verify-$P-2" "$BID")"
poll "$(printf "$FUNDING_STATUS" $P)" "CONFIRMED" 15 "모금 개설 확정(CONFIRMED)"
poll "$(printf "$SURVEY_STATUS" $P)" "IN_PROGRESS" 15 "설문 활성화(IN_PROGRESS)"

echo "· 중복 settled 재발행 → CAS 패배 no-op, 상태 불변이어야 함"
publish payment-succeeded "ord-verify-$P-2" "$(succeeded_event "ord-verify-$P-2" "$BID")"
sleep 5
assert_eq "재수신 후에도 CONFIRMED 유지" "CONFIRMED" "$(sql "$(printf "$FUNDING_STATUS" $P)")"
cleanup $P

# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══ S2. 기한 만료 → 무산 → CANCEL 팬아웃 · 이중 적재 방어 ═══"
P=cccccccc
cleanup $P
seed_funding $P "NOW() - INTERVAL 1 HOUR"
seed_participant $P 1 SETTLED
seed_participant $P 2 SETTLED
seed_order $P 1
seed_order $P 2

FID="$P-0000-0000-0000-000000000001"; SID="$P-0000-0000-0000-000000000002"; BID="$P-0000-0000-0000-000000000005"

echo "· 스케줄러 주기(최대 60초+) 대기 — tryFail 승자 발행 → 리스너 CANCEL 적재"
poll "$(printf "$FUNDING_STATUS" $P)" "FAILED" 90 "무산 확정(FAILED)"
poll "$(printf "$CANCEL_COUNT" $P)" "2" 20 "CANCEL 커맨드 2건 적재"

echo "· failed 수동 재발행 → exists+UNIQUE 흡수로 추가 적재 0건이어야 함"
publish co-funding-expired "$FID" "$(failed_event "$FID" "$SID" "[\"ord-verify-$P-1\",\"ord-verify-$P-2\"]")"
sleep 5
assert_eq "재발행 후에도 CANCEL 2건 유지" "2" "$(sql "$(printf "$CANCEL_COUNT" $P)")"
echo "  (수렴 FAILED→REFUNDED 는 payment 릴레이의 토스 취소 → ⑦ payment-refunded → 모금 ⑦ 리스너 소관 — 본 스크립트 범위 밖)"
cleanup $P

# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══ S3. 경합 상호배타 — 만료된 모금에 결제 확정 도착: 개설 xor 무산 ═══"
P=dddddddd
cleanup $P
seed_survey $P
seed_funding $P "NOW() - INTERVAL 1 SECOND"   # 방금 만료 — 스케줄러와 겨룰 창
seed_participant $P 1 SETTLED
seed_participant $P 2 SETTLED
seed_order $P 1
seed_order $P 2

FID="$P-0000-0000-0000-000000000001"; SID="$P-0000-0000-0000-000000000002"; BID="$P-0000-0000-0000-000000000005"
publish payment-succeeded "ord-verify-$P-2" "$(succeeded_event "ord-verify-$P-2" "$BID")"

echo "· 두 CAS(tryConfirm vs tryFail)가 같은 행을 두고 겨룸 — 한쪽만 성립해야 함"
deadline=$((SECONDS + 90)); final=""
while [ $SECONDS -lt $deadline ]; do
  final=$(sql "$(printf "$FUNDING_STATUS" $P)")
  [ "$final" != "FUNDING" ] && break
  sleep 2
done
survey=$(sql "$(printf "$SURVEY_STATUS" $P)")
cancels=$(sql "$(printf "$CANCEL_COUNT" $P)")
echo "  관측: 모금=$final, 설문=$survey, CANCEL=$cancels"
if [ "$final" = "CONFIRMED" ]; then
  # 설문 활성화는 ⑤ 발행→소비를 거치므로 모금 CONFIRMED 직후엔 아직 반영 전이다 — 폴링으로 기다린다.
  poll "$(printf "$SURVEY_STATUS" $P)" "IN_PROGRESS" 30 "개설 승자 — 설문 활성화"
  sleep 65   # 뒤늦은 스케줄러가 이겨서는 안 됨
  assert_eq "무산 CAS 패배 확인(CONFIRMED 유지)" "CONFIRMED" "$(sql "$(printf "$FUNDING_STATUS" $P)")"
elif [ "$final" = "FAILED" ] || [ "$final" = "REFUNDED" ]; then
  assert_eq "무산 승자 — 설문 미활성" "NOT_STARTED" "$survey"
  poll "$(printf "$CANCEL_COUNT" $P)" "2" 20 "환불 팬아웃 2건"
else
  FAIL=$((FAIL + 1)); echo "  ❌ 종착 미도달 (FUNDING 그대로)"
fi
cleanup $P

# ────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══ 결과: PASS=$PASS, FAIL=$FAIL ═══"
[ $FAIL -eq 0 ]
