import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { SharedArray } from "k6/data";

/**
 * Lost Update 검증 k6 스크립트
 *
 * 시나리오: 20명의 참가자가 동시에 20장의 티켓에 추첨 요청
 * - 티켓이 20장이므로 정상이면 20명 모두 성공해야 함
 * - Lost Update 발생 시: 같은 티켓을 두 명이 선택 → selectedTicketCount 불일치
 *
 * 검증 포인트:
 * 1. 성공 응답 수 == 실제 DB의 selectedTicketCount
 * 2. 하나의 티켓에 두 명이 당첨되는 경우가 없음
 * 3. 동시 요청 후 DrawingBoard 상태가 일관성 있음
 */

// ============================================================
// 설정 - 실제 환경에 맞게 수정하세요
// ============================================================
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SURVEY_ID = __ENV.SURVEY_ID || "테스트용-설문-UUID";

// 참가자 ID 목록 (테스트 데이터에 맞게 수정)
const PARTICIPANT_IDS = JSON.parse(
  __ENV.PARTICIPANT_IDS ||
    JSON.stringify([
      "participant-uuid-01",
      "participant-uuid-02",
      "participant-uuid-03",
      "participant-uuid-04",
      "participant-uuid-05",
      "participant-uuid-06",
      "participant-uuid-07",
      "participant-uuid-08",
      "participant-uuid-09",
      "participant-uuid-10",
      "participant-uuid-11",
      "participant-uuid-12",
      "participant-uuid-13",
      "participant-uuid-14",
      "participant-uuid-15",
      "participant-uuid-16",
      "participant-uuid-17",
      "participant-uuid-18",
      "participant-uuid-19",
      "participant-uuid-20",
    ])
);

// ============================================================
// 커스텀 메트릭
// ============================================================
const drawSuccess = new Counter("draw_success"); // 추첨 성공 횟수
const drawFail = new Counter("draw_fail"); // 추첨 실패 횟수 (중복/에러)
const drawDuplicate = new Counter("draw_duplicate"); // 중복 참여 에러
const drawTooManyRequests = new Counter("draw_too_many_requests"); // 락 획득 실패
const drawLatency = new Trend("draw_latency"); // 추첨 응답 시간

// ============================================================
// k6 옵션 - 20명이 동시에 요청
// ============================================================
export const options = {
  scenarios: {
    // 시나리오 1: 서로 다른 참가자가 서로 다른 번호로 동시 추첨
    concurrent_different_users: {
      executor: "shared-iterations",
      vus: 20,
      iterations: 20,
      maxDuration: "30s",
      exec: "differentUsersDifferentTickets",
    },
  },
  thresholds: {
    draw_success: ["count>=1"], // 최소 1건 이상 성공
    http_req_duration: ["p(95)<5000"], // 95% 요청이 5초 이내
  },
};

// ============================================================
// 시나리오: 서로 다른 참가자 × 서로 다른 티켓 번호
// 정상이면 모두 성공해야 하지만, Lost Update 시 일부 실패
// ============================================================
export function differentUsersDifferentTickets() {
  const vuIndex = __VU - 1; // 0-based index
  const participantId = PARTICIPANT_IDS[vuIndex % PARTICIPANT_IDS.length];
  const selectedNumber = vuIndex; // 각 VU가 다른 티켓 번호 선택

  const payload = JSON.stringify({
    participantId: participantId,
    selectedNumber: selectedNumber,
    phoneNumber: `010-0000-${String(vuIndex).padStart(4, "0")}`,
  });

  const params = {
    headers: { "Content-Type": "application/json" },
    tags: { scenario: "different_users" },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/drawing-board/draw`,
    payload,
    params
  );

  drawLatency.add(res.timings.duration);

  const isSuccess = check(res, {
    "상태코드 200": (r) => r.status === 200,
  });

  if (isSuccess) {
    drawSuccess.add(1);
    const body = JSON.parse(res.body);
    console.log(
      `[VU${__VU}] 추첨 성공 - 참가자: ${participantId}, 번호: ${selectedNumber}, 당첨: ${body.isWon}`
    );
  } else {
    drawFail.add(1);

    // 에러 유형 분류
    if (res.status === 400 && res.body.includes("이미 참여")) {
      drawDuplicate.add(1);
      console.log(
        `[VU${__VU}] 중복 참여 에러 - 참가자: ${participantId}, 번호: ${selectedNumber}`
      );
    } else if (res.status === 429 || res.body.includes("동시 요청")) {
      drawTooManyRequests.add(1);
      console.log(
        `[VU${__VU}] 락 대기 초과 - 참가자: ${participantId}, 번호: ${selectedNumber}`
      );
    } else {
      console.log(
        `[VU${__VU}] 기타 에러 - status: ${res.status}, body: ${res.body}`
      );
    }
  }
}

// ============================================================
// 테스트 종료 후 DrawingBoard 상태 검증
// ============================================================
export function handleSummary(data) {
  // DrawingBoard 조회로 최종 상태 확인
  const res = http.get(
    `${BASE_URL}/api/v1/drawing-board/info/${SURVEY_ID}`
  );

  let verification = "=== Lost Update 검증 결과 ===\n\n";

  const successCount =
    data.metrics.draw_success?.values?.count || 0;
  const failCount = data.metrics.draw_fail?.values?.count || 0;

  verification += `추첨 성공: ${successCount}건\n`;
  verification += `추첨 실패: ${failCount}건\n`;
  verification += `총 요청: ${successCount + failCount}건\n\n`;

  if (res.status === 200) {
    const board = JSON.parse(res.body);
    const selectedCount = board.selectedTicketCount;
    const ticketSelected = board.tickets.filter((t) => t === true).length;

    verification += `DB selectedTicketCount: ${selectedCount}\n`;
    verification += `실제 선택된 티켓 수: ${ticketSelected}\n\n`;

    // 핵심 검증: 성공 응답 수 == DB 상태
    if (successCount === selectedCount) {
      verification += `✅ 정합성 일치 - Lost Update 없음\n`;
      verification += `   API 성공(${successCount}) == DB 카운트(${selectedCount})\n`;
    } else {
      verification += `❌ Lost Update 감지!\n`;
      verification += `   API 성공(${successCount}) != DB 카운트(${selectedCount})\n`;
      verification += `   차이: ${Math.abs(successCount - selectedCount)}건의 데이터 불일치\n`;
    }

    // 추가 검증: 티켓 배열과 카운트 일치
    if (selectedCount !== ticketSelected) {
      verification += `\n❌ 내부 불일치! selectedTicketCount(${selectedCount}) != 실제 선택 티켓(${ticketSelected})\n`;
    }
  } else {
    verification += `⚠️ DrawingBoard 조회 실패: ${res.status}\n`;
  }

  verification += `\n=== 면접 포인트 ===\n`;
  verification += `Q: "@RedissonLock과 @Transactional 순서가 보장되지 않으면 어떤 문제가 생기나?"\n`;
  verification += `A: "트랜잭션 커밋 전에 락이 해제되어 다른 스레드가 커밋되지 않은 데이터를 기반으로\n`;
  verification += `   작업할 수 있다. 현재 구현은 TransactionSynchronization.afterCompletion()으로\n`;
  verification += `   트랜잭션 커밋 후에만 unlock하여 이 문제를 방지한다."\n`;

  return {
    stdout: verification,
    "k6_scripts/result-lost-update.json": JSON.stringify(data, null, 2),
  };
}
