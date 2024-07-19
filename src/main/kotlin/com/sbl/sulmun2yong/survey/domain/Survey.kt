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
        if (sections.isEmpty()) throw InvalidSurveyException()
        if (publishedAt == null && status != SurveyStatus.NOT_STARTED) throw InvalidSurveyException()
        if (publishedAt != null && publishedAt.after(finishedAt)) throw InvalidSurveyException()
        if (targetParticipants < getRewardCount()) throw InvalidSurveyException()
        sections.map { it.validateRouteDetails() }
    }

    fun validateResponse(sectionResponses: List<SectionResponse>) {
        var currentSectionId: UUID? = sections.first().id
        for (sectionResponse in sectionResponses) {
            if (currentSectionId != sectionResponse.sectionId) throw InvalidSurveyResponseException()
            val section = sections.find { it.id == currentSectionId } ?: throw InvalidSurveyResponseException()
            currentSectionId = section.findNextSectionId(sectionResponse.questionResponses)
        }
        if (currentSectionId != null) throw InvalidSurveyResponseException()
    }

    fun getRewardCount(): Int {
        return rewards.sumOf { it.count }
    }

    private fun Section.validateRouteDetails() {
        val isRouteDetailsValid = this.routeDetails.isRouteDetailsSectionIdValid(sectionIdSet)
        if (!isRouteDetailsValid) throw InvalidSurveyException()
    }
}
