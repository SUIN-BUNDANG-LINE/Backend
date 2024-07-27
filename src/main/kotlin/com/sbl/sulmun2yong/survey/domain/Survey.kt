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
    val targetParticipantCount: Int,
    val rewards: List<Reward>,
    val sections: List<Section>,
) {
    private val sectionIdSet = sections.map { it.id }.toSet()

    init {
        validateSurvey()
    }

    private fun validateSurvey() {
        require(sections.isNotEmpty()) { throw InvalidSurveyException() }
        require(isDestinationSectionIdContainsAll()) { throw InvalidSurveyException() }
        require(isSectionsUnique()) { throw InvalidSurveyException() }
        require(isSurveyStatusValid()) { throw InvalidSurveyException() }
        require(isFinishedAtAfterPublishedAt()) { throw InvalidSurveyException() }
        require(isTargetParticipantsEnough()) { throw InvalidSurveyException() }
    }

    fun validateResponse(surveyResponse: SurveyResponse) {
        require(surveyResponse.surveyId == id) { throw InvalidSurveyResponseException() }
        var currentSectionId: UUID? = sections.first().id
        for (sectionResponse in surveyResponse) {
            require(currentSectionId == sectionResponse.sectionId) { throw InvalidSurveyResponseException() }
            val section = findSectionById(currentSectionId) ?: throw InvalidSurveyResponseException()
            currentSectionId = section.findNextSectionId(sectionResponse)
        }
        require(currentSectionId == null) { throw InvalidSurveyResponseException() }
    }

    fun getRewardCount() = rewards.sumOf { it.count }

    private fun isDestinationSectionIdContainsAll() =
        sections.all { sectionIdSet.containsAll(it.getDestinationSectionIdSet().filterNotNull()) }

    private fun isSectionsUnique() = sections.size == sectionIdSet.size

    private fun isSurveyStatusValid() = publishedAt != null || status == SurveyStatus.NOT_STARTED

    private fun isFinishedAtAfterPublishedAt() = publishedAt == null || finishedAt.after(publishedAt)

    private fun isTargetParticipantsEnough() = targetParticipantCount >= getRewardCount()

    private fun findSectionById(sectionId: UUID) = sections.find { it.id == sectionId }
}
