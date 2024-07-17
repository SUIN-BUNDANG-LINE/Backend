package com.sbl.sulmun2yong.survey.domain

import java.util.Date
import java.util.UUID

data class Survey(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String,
    val publishedAt: Date?,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipants: Int,
    val rewards: List<Reward>,
    val sections: List<Section>,
) {
    init {
        // TODO: publishedAt이 finishedAt보다 나중일 수 없다.
        // TODO: publishedAt은 SurveyStatus가 NOT_STARTED일 때만 null이다.
        // TODO: 섹션은 하나 이상 존재해야한다.
    }

    fun validateResponse(surveyResponses: List<SurveyResponse>) {
        // TODO: 응답한 섹션 순서가 유효한지 검증
    }

    fun getRewardCount(): Int {
        return rewards.sumOf { it.count }
    }
}
