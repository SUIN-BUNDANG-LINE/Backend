package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import com.sbl.sulmun2yong.payment.entity.PaymentOrderStatus
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyStartResponse
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SurveyWorkbenchService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
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

    @Transactional
    fun startSurvey(
        surveyId: UUID,
        makerId: UUID,
    ): SurveyStartResponse {
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }

        return when {
            // 경품 설문 최초 시작 -> 바로 열지 않고 결제 대기로: 주문 적재 + PENDING_PAYMENT(한 트랜잭션)
            survey.rewardSetting is ImmediateDrawSetting && survey.status == SurveyStatus.NOT_STARTED
            -> {
                val drawingBoard =
                    drawingBoardRepository.findBySurveyId(survey.id).orElseGet {
                        drawingBoardRepository.save(
                            DrawingBoard.create(
                                surveyId = survey.id,
                                boardSize = survey.rewardSetting.targetParticipantCount!!,
                                rewards = survey.rewardSetting.rewards,
                            ),
                        )
                    }

                val amount = rewardUnitPrice * survey.rewardSetting.rewards.sumOf { it.count }
                val order =
                    paymentOrderRepository
                        .findBySurveyId(survey.id)
                        .map { existing ->
                            // 이전 주문이 결제 가능 상태(PENDING)가 아니면 새 orderId로 되살린다 (재결제)
                            if (existing.status != PaymentOrderStatus.PENDING) {
                                existing.renew("ord-${UUID.randomUUID()}", amount)
                            }
                            existing
                        }.orElseGet {
                            paymentOrderRepository.save(
                                PaymentOrder.create(
                                    surveyId = survey.id,
                                    makerId = makerId,
                                    orderId = "ord-${UUID.randomUUID()}",
                                    amount = amount,
                                ),
                            )
                        }

                surveyRepository.save(survey.awaitPayment())
                SurveyStartResponse(
                    paymentRequired = true,
                    checkoutUrl = "/payments/checkout.html?orderId=${order.orderId}",
                )
            }

            // 결제 대기 중 재호출 - 설문을 열지 않고(!) 기존 주문으로 결제 페이지 안내
            survey.status == SurveyStatus.PENDING_PAYMENT -> {
                val order = paymentOrderRepository.findBySurveyId(survey.id).orElseThrow()
                SurveyStartResponse(
                    paymentRequired = true,
                    checkoutUrl = "/payments/checkout.html?orderId=${order.orderId}",
                )
            }

            // 결제가 필요 없는 경우(수정 재개 등) - 기존 즉시 시작
            else -> {
                surveyRepository.save(survey.start())
                SurveyStartResponse(paymentRequired = false, checkoutUrl = null)
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
