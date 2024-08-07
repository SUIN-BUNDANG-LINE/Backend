package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveySaveResponse
import org.springframework.stereotype.Service
import java.util.UUID

// TODO: 추후에 패키지 구조를 변경하여 Service가 특정 도메인이 아닌 요청에 종속되도록 하기
@Service
class SurveyManagementService(
    private val surveyAdapter: SurveyAdapter,
    private val drawingBoardAdapter: DrawingBoardAdapter,
) {
    fun createSurvey(makerId: UUID): SurveyCreateResponse {
        val survey = Survey.create(makerId)
        surveyAdapter.save(survey)
        return SurveyCreateResponse(surveyId = survey.id)
    }

    // TODO: 수정할 수 있는 설문의 정보에 제한이 필요
    // TODO: 추첨 보드 생성 로직을 startSurvey로 옮기기
    // TODO: Reward 객체를 SurveyDomain의 것으로 통일하기
    fun saveSurvey(
        surveySaveRequest: SurveySaveRequest,
        makerId: UUID,
    ): SurveySaveResponse? {
        val sectionIds = SectionIds.from(surveySaveRequest.sections.map { SectionId.Standard(it.id) })
        val rewards = surveySaveRequest.rewards.map { it.toSurveyDomain() }
        val survey =
            with(surveySaveRequest) {
                Survey(
                    id = surveySaveRequest.id,
                    title = this.title,
                    description = this.description,
                    thumbnail = this.thumbnail,
                    publishedAt = this.publishedAt,
                    finishedAt = this.finishedAt,
                    status = this.status,
                    finishMessage = this.finishMessage,
                    targetParticipantCount = surveySaveRequest.targetParticipantCount,
                    makerId = makerId,
                    rewards = rewards,
                    sections = this.sections.map { it.toDomain(sectionIds) },
                )
            }
        surveyAdapter.save(survey)

        val drawingBoard =
            DrawingBoard.create(
                survey.id,
                surveySaveRequest.targetParticipantCount,
                surveySaveRequest.rewards.map {
                    it.toDrawingDomain()
                },
            )
        drawingBoardAdapter.save(drawingBoard)

        return SurveySaveResponse(surveyId = survey.id)
    }

    fun getSurveyMakeInfo(surveyId: UUID) = SurveyMakeInfoResponse.of(surveyAdapter.getSurvey(surveyId))
}
