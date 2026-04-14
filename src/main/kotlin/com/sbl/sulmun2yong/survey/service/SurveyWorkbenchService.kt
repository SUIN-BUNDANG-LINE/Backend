package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// TODO: 추후에 패키지 구조를 변경하여 Service가 특정 도메인이 아닌 요청에 종속되도록 하기
@Service
class SurveyWorkbenchService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    fun createSurvey(makerId: UUID): SurveyCreateResponse {
        val survey = Survey.create(makerId)
        surveyRepository.save(survey)
        return SurveyCreateResponse(surveyId = survey.id)
    }

    fun saveSurvey(
        surveyId: UUID,
        surveySaveRequest: SurveySaveRequest,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }
        val newSurvey =
            with(surveySaveRequest) {
                survey.updateContent(
                    title = this.title,
                    description = this.description,
                    thumbnail = this.thumbnail,
                    finishMessage = this.finishMessage,
                    rewardSetting = this.rewardSetting.toDomain(survey.status),
                    isVisible = this.isVisible,
                    isResultOpen = this.isResultOpen,
                    sections = this.sections.toDomain(),
                )
            }
        surveyRepository.save(newSurvey)
    }

    @Transactional
    fun startSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }
        val startedSurvey = survey.start()
        surveyRepository.save(startedSurvey)
        // 즉시 추첨이면서 최초 시작 시 추첨 보드 생성
        if (startedSurvey.rewardSetting is ImmediateDrawSetting && survey.status == SurveyStatus.NOT_STARTED) {
            val drawingBoard =
                DrawingBoard.create(
                    surveyId = startedSurvey.id,
                    boardSize = startedSurvey.rewardSetting.targetParticipantCount!!,
                    rewards = startedSurvey.rewardSetting.rewards,
                )
            drawingBoardRepository.save(drawingBoard)
        }
    }

    fun editSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }
        surveyRepository.save(survey.edit())
    }

    fun finishSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }
        surveyRepository.save(survey.finish())
    }

    fun deleteSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val isSuccess = surveyRepository.softDelete(surveyId, makerId)
        if (!isSuccess) throw SurveyNotFoundException()
    }
}
