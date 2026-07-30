package com.sbl.sulmun2yong.survey.dto.response

import java.util.UUID

data class SurveyParticipantResponse(
    val participantId: UUID,
    /** 즉시 추첨 방식인 경우 True -> False면 추첨 페이지 스킵 */
    val isImmediateDraw: Boolean,
)
