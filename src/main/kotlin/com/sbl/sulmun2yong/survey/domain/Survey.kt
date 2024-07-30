package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import java.util.Date
import java.util.UUID

// TODO: 설문 일정 관련 속성들을 하나의 클래스로 묶기
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
    init {
        validateSurvey()
    }

    private fun validateSurvey() {
        require(sections.isNotEmpty()) { throw InvalidSurveyException() }
        require(isSectionsUnique()) { throw InvalidSurveyException() }
        require(isSurveyStatusValid()) { throw InvalidSurveyException() }
        require(isFinishedAtAfterPublishedAt()) { throw InvalidSurveyException() }
        require(isTargetParticipantsEnough()) { throw InvalidSurveyException() }
        require(isSectionIdsValid()) { throw InvalidSurveyException() }
    }

    fun validateResponse(surveyResponse: SurveyResponse) {
        var currentSectionId: SectionId = sections.first().id
        for (sectionResponse in surveyResponse) {
            val responseSectionId = sectionResponse.sectionId
            require(currentSectionId == responseSectionId) { throw InvalidSurveyResponseException() }
            val section = findSectionById(responseSectionId) ?: throw InvalidSurveyResponseException()
            currentSectionId = section.findNextSectionId(sectionResponse)
        }
        require(currentSectionId.isEnd) { throw InvalidSurveyResponseException() }
    }

    fun getRewardCount() = rewards.sumOf { it.count }

    private fun isSectionsUnique() = sections.size == sections.distinctBy { it.id }.size

    private fun isSurveyStatusValid() = publishedAt != null || status == SurveyStatus.NOT_STARTED

    private fun isFinishedAtAfterPublishedAt() = publishedAt == null || finishedAt.after(publishedAt)

    private fun isTargetParticipantsEnough() = targetParticipantCount >= getRewardCount()

    private fun isSectionIdsValid(): Boolean {
        val sectionIds = SectionIds(sections.map { it.id } + SectionId.End)
        return sections.all { it.sectionIds == sectionIds }
    }

    private fun findSectionById(sectionId: SectionId.Standard) = sections.find { it.id == sectionId }
}
