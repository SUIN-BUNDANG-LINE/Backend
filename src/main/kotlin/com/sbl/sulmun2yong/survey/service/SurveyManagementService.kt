package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.service.DrawingBoardService
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.dto.SurveyCreateRequest
import com.sbl.sulmun2yong.survey.dto.SurveyCreateResponse
import org.springframework.stereotype.Service

@Service
class SurveyManagementService(
    private val surveyAdapter: SurveyAdapter,
    private val drawingBoardService: DrawingBoardService,
) {
    fun createSurvey(surveyCreateRequest: SurveyCreateRequest): SurveyCreateResponse? {
        val sectionIds = SectionIds.from(surveyCreateRequest.sections.map { SectionId.Standard(it.id) })
        val survey =
            with(surveyCreateRequest) {
                Survey(
                    id = surveyCreateRequest.id,
                    title = this.title,
                    description = this.description,
                    thumbnail = this.thumbnail,
                    publishedAt = this.publishedAt,
                    finishedAt = this.finishedAt,
                    status = this.status,
                    finishMessage = this.finishMessage,
                    targetParticipantCount = surveyCreateRequest.targetParticipantCount,
                    rewards = surveyCreateRequest.rewards.map { it.toSurveyDomain() },
                    sections = this.sections.map { it.toDomain(sectionIds) },
                )
            }
        drawingBoardService.makeDrawingBoard(
            surveyId = surveyCreateRequest.id,
            boardSize = surveyCreateRequest.targetParticipantCount,
            surveyRewards = surveyCreateRequest.rewards.map { it.toRewardDomain() },
        )
        surveyAdapter.save(survey)
        return SurveyCreateResponse(surveyId = survey.id)
    }
}
