package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.entity.DrawingBoardStatus
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyStartResponse
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.publisher.SurveyOutboxKafkaPublisher
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SurveyWorkbenchService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val sagaPublisher: SurveyOutboxKafkaPublisher,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
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
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }
        // 결제 대기 보드가 살아 있으면 수정 잠금 - 경품 스냅숏·총액이 현재 설정에 묶여 있다
        val paymentLocked =
            drawingBoardRepository.existsBySurveyIdAndStatus(surveyId, DrawingBoardStatus.PENDING_PAYMENT)
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
                    paymentLocked = paymentLocked,
                )
            }
        surveyRepository.save(newSurvey)
    }

    // 설문 시작 - 유료(경품) 설문이면 여기서 열지 않는다. 유료 개시의 유일한 길은 모금 접수
    // (POST /surveys/{id}/co-funding, 1인 = 단독)이고, 보드 생성·주문 발급·개시는 그 사가(판정 tx·③·⑤)의 몫.
    // 이 메서드는 결제 필요 신호(paymentRequired)만 돌려주고, 무료·수정 재개 설문만 즉시 연다.
    @Transactional
    fun startSurvey(
        surveyId: UUID,
        makerId: UUID,
    ): SurveyStartResponse {
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }

        val rewardSetting = survey.rewardSetting
        return when {
            // 경품 설문 개시는 모금 접수(1인 = 단독)로 일원화 - 결제 필요 신호만 돌려준다.
            // 보드 생성·주문 발급은 접수 사가(판정 tx·③)가 담당한다.
            rewardSetting is ImmediateDrawSetting && survey.status == SurveyStatus.NOT_STARTED -> {
                SurveyStartResponse(paymentRequired = true)
            }

            // 결제가 필요 없는 경우(수정 재개 등) - 기존 즉시 시작
            else -> {
                surveyRepository.save(survey.start())
                SurveyStartResponse(paymentRequired = false)
            }
        }
    }

    fun editSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }
        surveyRepository.save(survey.edit())
    }

    fun finishSurvey(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }
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
