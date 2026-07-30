package com.sbl.sulmun2yong.drawing.dto.event

import java.time.Instant

/**
 * 백엔드가 `drawing-completed` 토픽으로 발행하는 추첨 완료 이벤트의 정본(正本) 스키마.
 *
 * 각 컨슈머는 소비 시점엔 필요한 필드만 담은 자기 슬림 payload 사본으로 역직렬화한다.
 * 반면 SMS 문구를 만들 때는 보상명·당첨번호까지 필요하므로, notification 컨슈머가
 * SMS 잡에 저장해 둔 원본 payload 를 이 전체 스키마로 되살려 발송에 사용한다.
 */
data class DrawingCompletedEvent(
    val eventId: String,
    val surveyId: String,
    val participantId: String,
    val selectedNumber: Int,
    val isWinner: Boolean,
    val rewardName: String?,
    val rewardCategory: String?,
    val remainingTickets: Int,
    val timestamp: Instant = Instant.now(),
)
