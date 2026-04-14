import http from "k6/http";
import { check } from "k6";
import { Counter, Trend } from "k6/metrics";

/**
 * Same Ticket Race Condition 테스트
 *
 * 시나리오: 10명의 참가자가 동시에 같은 번호(0번)의 티켓을 선택
 * - 정상(락 동작): 1명만 성공, 나머지 9명은 "이미 선택된 티켓" 에러
 * - Lost Update 발생 시: 2명 이상이 성공 → selectedTicketCount가 1인데 실제 2명이 당첨
 *
 * 이 테스트가 가장 직접적으로 Lost Update를 드러냄:
 * - Thread A: 티켓 0번 읽기(미선택) → 선택 처리 → 커밋 전 락 해제
 * - Thread B: 티켓 0번 읽기(미선택, A 커밋 전) → 선택 처리 → 커밋
 * - Thread A: 커밋 → 둘 다 성공 = Lost Update
 */

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SURVEY_ID = __ENV.SURVEY_ID || "테스트용-설문-UUID";

const PARTICIPANT_IDS = JSON.parse(
  __ENV.PARTICIPANT_IDS ||
    JSON.stringify([
      "race-participant-01",
      "race-participant-02",
      "race-participant-03",
      "race-participant-04",
      "race-participant-05",
      "race-participant-06",
      "race-participant-07",
      "race-participant-08",
      "race-participant-09",
      "race-participant-10",
    ])
);

// 모든 참가자가 같은 티켓 번호를 선택
const TARGET_TICKET = 0;

// 커스텀 메트릭
const raceSuccess = new Counter("race_success");
const raceFail = new Counter("race_fail");
const raceAlreadySelected = new Counter("race_already_selected");
const raceTooMany = new Counter("race_too_many_requests");

export const options = {
  scenarios: {
    same_ticket_race: {
      executor: "shared-iterations",
      vus: 10,
      iterations: 10,
      maxDuration: "30s",
      exec: "sameTicketRace",
    },
  },
};

export function sameTicketRace() {
  const vuIndex = __VU - 1;
  const participantId = PARTICIPANT_IDS[vuIndex % PARTICIPANT_IDS.length];

  const payload = JSON.stringify({
    participantId: participantId,
    selectedNumber: TARGET_TICKET,
    phoneNumber: `010-1111-${String(vuIndex).padStart(4, "0")}`,
  });

  const params = {
    headers: { "Content-Type": "application/json" },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/drawing-board/draw`,
    payload,
    params
  );

  const isSuccess = check(res, {
    "상태코드 200": (r) => r.status === 200,
  });

  if (isSuccess) {
    raceSuccess.add(1);
    console.log(`[VU${__VU}] ✅ 같은 티켓(${TARGET_TICKET}) 추첨 성공 - ${participantId}`);
  } else {
    raceFail.add(1);
    if (res.body && res.body.includes("이미 선택")) {
      raceAlreadySelected.add(1);
      console.log(`[VU${__VU}] 🎫 이미 선택된 티켓 - ${participantId}`);
    } else if (res.body && res.body.includes("동시 요청")) {
      raceTooMany.add(1);
      console.log(`[VU${__VU}] ⏳ 락 대기 초과 - ${participantId}`);
    } else {
      console.log(
        `[VU${__VU}] ❌ 에러 - status: ${res.status}, body: ${res.body}`
      );
    }
  }
}

export function handleSummary(data) {
  const res = http.get(
    `${BASE_URL}/api/v1/drawing-board/info/${SURVEY_ID}`
  );

  let result = "=== Same Ticket Race Condition 검증 ===\n\n";

  const successCount = data.metrics.race_success?.values?.count || 0;
  const failCount = data.metrics.race_fail?.values?.count || 0;

  result += `같은 티켓(${TARGET_TICKET}번)에 동시 요청: ${successCount + failCount}건\n`;
  result += `성공: ${successCount}건\n`;
  result += `실패: ${failCount}건\n\n`;

  if (successCount <= 1) {
    result += `✅ 정상 - 같은 티켓에 최대 1명만 성공\n`;
    result += `   분산 락이 정상적으로 동시성을 제어하고 있음\n`;
  } else {
    result += `❌ Lost Update 발생!\n`;
    result += `   같은 티켓에 ${successCount}명이 성공 → 데이터 불일치\n`;
    result += `   원인: 트랜잭션 커밋 전에 락이 해제되어 다른 스레드가 stale 데이터를 읽음\n`;
  }

  if (res.status === 200) {
    const board = JSON.parse(res.body);
    result += `\nDB 상태: selectedTicketCount = ${board.selectedTicketCount}\n`;
    result += `티켓 ${TARGET_TICKET}번 선택 여부: ${board.tickets[TARGET_TICKET]}\n`;
  }

  result += `\n=== 핵심 포인트 ===\n`;
  result += `현재 구현: RedissonLockAspect가 TransactionSynchronization으로\n`;
  result += `트랜잭션 커밋 후에 unlock → Lost Update 방지\n\n`;
  result += `만약 @Order 없이 락 AOP가 트랜잭션 안쪽에서 실행되면:\n`;
  result += `  Lock 획득 → 비즈니스 로직 → Lock 해제 → 트랜잭션 커밋\n`;
  result += `  이 사이에 다른 스레드가 커밋 전 데이터를 읽어 Lost Update 발생\n`;

  return {
    stdout: result,
    "k6_scripts/result-same-ticket-race.json": JSON.stringify(data, null, 2),
  };
}
