package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyAccessException
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

    fun saveSurvey(
        surveyId: UUID,
        surveySaveRequest: SurveySaveRequest,
        makerId: UUID,
    ) {
        val survey = surveyAdapter.getSurvey(surveyId)
        // 현재 유저와 설문 제작자가 다를 경우 예외 발생
        if (survey.makerId != makerId) throw InvalidSurveyAccessException()
        val newSurvey =
            with(surveySaveRequest) {
                survey.updateContent(
                    title = this.title,
                    description = this.description,
                    thumbnail = this.thumbnail,
                    finishMessage = this.finishMessage,
                    rewardSetting = this.rewardSetting.toDomain(survey.status),
                    isVisible = this.isVisible,
                    sections = this.sections.toDomain(),
                )
            }
        surveyAdapter.save(newSurvey)
    }

    fun getSurveyMakeInfo(
        surveyId: UUID,
        makerId: UUID,
    ): SurveyMakeInfoResponse {
        val survey = surveyAdapter.getSurvey(surveyId)
        // 현재 유저와 설문 제작자가 다를 경우 예외 발생
        if (survey.makerId != makerId) throw InvalidSurveyAccessException()
        return SurveyMakeInfoResponse.of(survey)
    }

    // TODO: 트랜잭션 적용 필요
    fun startSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey = surveyAdapter.getSurvey(surveyId)
        // 현재 유저와 설문 제작자가 다를 경우 예외 발생
        if (survey.makerId != makerId) throw InvalidSurveyAccessException()
        val startedSurvey = survey.start()
        surveyAdapter.save(startedSurvey)
        if (startedSurvey.rewardSetting is ImmediateDrawSetting) {
            val drawingBoard =
                DrawingBoard.create(
                    surveyId = startedSurvey.id,
                    boardSize = startedSurvey.rewardSetting.targetParticipantCount,
                    rewards = startedSurvey.rewardSetting.rewards,
                )
            drawingBoardAdapter.save(drawingBoard)
        }
    }

    fun editSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey = surveyAdapter.getSurvey(surveyId)
        // 현재 유저와 설문 제작자가 다를 경우 예외 발생
        if (survey.makerId != makerId) throw InvalidSurveyAccessException()
        surveyAdapter.save(survey.edit())
    }

    fun finishSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey = surveyAdapter.getByIdAndMakerId(surveyId, makerId)
        surveyAdapter.save(survey.finish())
    }
}
