package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
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
    private val sectionIdSet = sections.map { it.id }.toSet()

    init {
        validateSurvey()
    }

    private fun validateSurvey() {
        require(sections.isNotEmpty()) { throw InvalidSurveyException() }
        // 각 섹션의 경로 설정에 해당하는 섹션 ID들은 유효해야한다.
        require(isAllRouteDetailsValid()) { throw InvalidSurveyException() }
        require(sections.size == sectionIdSet.size) { throw InvalidSurveyException() }
        require(publishedAt != null || status == SurveyStatus.NOT_STARTED) { throw InvalidSurveyException() }
        require(publishedAt == null || finishedAt.after(publishedAt)) { throw InvalidSurveyException() }
        require(targetParticipants >= getRewardCount()) { throw InvalidSurveyException() }
    }

    // 설문의 응답에 해당하는 섹션이 유효한지, 설문의 흐름이 유효한지 확인한다.
    fun validateResponse(surveyResponse: SurveyResponse) {
        var currentSectionId: UUID? = sections.first().id
        // 응답을 차례대로 확인하면서 유효한 응답인지, 섹션의 흐름이 올바른지 확인한다.
        for (sectionResponse in surveyResponse) {
            if (currentSectionId != sectionResponse.sectionId) throw InvalidSurveyResponseException()
            val section = findSectionById(currentSectionId) ?: throw InvalidSurveyResponseException()
            currentSectionId = section.findNextSectionId(sectionResponse)
        }
        // 최종적인 섹션 ID가 null이 아니면 끝까지 응답하지 않은 것이므로 예외를 발생시킨다.
        if (currentSectionId != null) throw InvalidSurveyResponseException()
    }

    fun getRewardCount() = rewards.sumOf { it.count }

    private fun isAllRouteDetailsValid() = sections.all { it.routeDetails.isRouteDetailsSectionIdValid(sectionIdSet) }

    private fun findSectionById(sectionId: UUID) = sections.find { it.id == sectionId }
}
