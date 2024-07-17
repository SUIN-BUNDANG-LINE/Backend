package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
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
        if (sections.isEmpty()) throw InvalidSurveyException()
        if (publishedAt == null && status != SurveyStatus.NOT_STARTED) throw InvalidSurveyException()
        if (publishedAt != null && publishedAt.after(finishedAt)) throw InvalidSurveyException()
        if (targetParticipants < getRewardCount()) throw InvalidSurveyException()
    }

    fun validateResponse(sectionResponses: List<SectionResponse>) {
        // TODO: 응답한 섹션 순서가 유효한지 검증
    }

    fun getRewardCount(): Int {
        return rewards.sumOf { it.count }
    }
}
