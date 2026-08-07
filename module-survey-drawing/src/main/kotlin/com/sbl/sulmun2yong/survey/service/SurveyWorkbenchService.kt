package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyStartResponse
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.publisher.SurveySagaPublisher
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SurveyWorkbenchService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val sagaPublisher: SurveySagaPublisher,
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

    // 단독(비모금) 개시 - 한 트랜잭션: 검증 + 보드 생성 + PENDING_PAYMENT 전이 + survey-payment-pending
    // Outbox 발행. 주문 발급(payment_orders 쓰기)은 이벤트 구독자(결제, origin=SOLO)의 몫이다.
    // checkoutUrl 은 surveyId 로 조립한다 - 주문 식별은 결제 자기 데이터라 설문은 orderId 를 모른다.
    // 발급이 이벤트 수렴 전이면 결제창의 주문 조회가 404 - 프론트가 재시도한다(최종적 일관성 창).
    // 재호출(PENDING_PAYMENT)도 재발행한다 - 발급이 멱등(설문당 1행)이라 안전하고, 자기치유가 된다.
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
            // 경품 설문 최초 시작 -> 바로 열지 않고 결제 대기로
            rewardSetting is ImmediateDrawSetting && survey.status == SurveyStatus.NOT_STARTED -> {
                // 경품 보드 사전 생성 - 설문·추첨은 같은 서비스 동거 도메인이라 직접 쓰기 유지(배포 경계 안)
                drawingBoardRepository.findBySurveyId(survey.id).orElseGet {
                    drawingBoardRepository.save(
                        DrawingBoard.create(
                            surveyId = survey.id,
                            boardSize = rewardSetting.targetParticipantCount!!,
                            rewards = rewardSetting.rewards,
                        ),
                    )
                }
                surveyRepository.save(survey.awaitPayment())
                publishPending(survey, rewardSetting)
                checkoutResponse(survey.id)
            }

            // 결제 대기 중 재호출 - 설문을 열지 않고(!) 결제 진입으로 재안내. 발급 유실 대비 재발행(멱등).
            survey.status == SurveyStatus.PENDING_PAYMENT -> {
                if (rewardSetting is ImmediateDrawSetting) publishPending(survey, rewardSetting)
                checkoutResponse(survey.id)
            }

            // 결제가 필요 없는 경우(수정 재개 등) - 기존 즉시 시작
            else -> {
                surveyRepository.save(survey.start())
                SurveyStartResponse(paymentRequired = false, checkoutUrl = null)
            }
        }
    }

    private fun publishPending(
        survey: Survey,
        rewardSetting: ImmediateDrawSetting,
    ) {
        sagaPublisher.publishPaymentPending(
            surveyId = survey.id.toString(),
            makerId = survey.makerId.toString(),
            amount = rewardUnitPrice * rewardSetting.rewards.sumOf { it.count },
        )
    }

    private fun checkoutResponse(surveyId: UUID) =
        SurveyStartResponse(
            paymentRequired = true,
            checkoutUrl = "/payments/checkout.html?surveyId=$surveyId",
        )

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
