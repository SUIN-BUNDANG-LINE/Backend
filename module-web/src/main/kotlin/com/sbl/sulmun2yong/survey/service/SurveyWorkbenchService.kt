package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.client.PaymentOrderClient
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@Service
class SurveyWorkbenchService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    // 교차 "읽기" 전용(재호출 시 기존 주문 조회) - 쓰기(발급)는 결제 내부 API(PaymentOrderClient) 몫
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentOrderClient: PaymentOrderClient,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

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

    // 결제 진입 준비 tx 의 산출물 - lazy 컬렉션(rewards) 접근·전이는 tx 안, HTTP 발급은 tx 밖.
    private data class CheckoutPrep(
        val paymentRequired: Boolean,
        val amount: Int = 0,
        val existingOrderId: String? = null,
    )

    // 오케스트레이션(tx 없음) - "tx 안 HTTP" 를 피한다. 주문 발급(payment_orders 쓰기)은
    // 결제 내부 API 몫(단일 기록자) - checkoutUrl 동기 반환 계약 유지.
    // 전이(PENDING_PAYMENT) 후 발급 HTTP 가 실패하면 "대기인데 주문 없음" 상태가 남지만,
    // 재호출 시 발급을 다시 시도(멱등)하므로 자기치유된다.
    fun startSurvey(
        surveyId: UUID,
        makerId: UUID,
    ): SurveyStartResponse {
        val prep =
            transactionTemplate.execute { prepareCheckout(surveyId, makerId) }
                ?: throw SurveyNotFoundException()

        if (!prep.paymentRequired) return SurveyStartResponse(paymentRequired = false, checkoutUrl = null)

        val orderId =
            prep.existingOrderId
                ?: paymentOrderClient.issueOrder(surveyId, makerId, prep.amount)
        return SurveyStartResponse(
            paymentRequired = true,
            checkoutUrl = "/payments/checkout.html?orderId=$orderId",
        )
    }

    // 짧은 tx: 분기 판단 + amount 계산(lazy rewards) + 보드 생성 + 상태 전이. 주문 쓰기는 없다.
    private fun prepareCheckout(
        surveyId: UUID,
        makerId: UUID,
    ): CheckoutPrep {
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId)
                .orElseThrow { SurveyNotFoundException() }

        return when {
            // 경품 설문 최초 시작 -> 바로 열지 않고 결제 대기로
            survey.rewardSetting is ImmediateDrawSetting && survey.status == SurveyStatus.NOT_STARTED
            -> {
                // 경품 보드 사전 생성 - 설문·추첨은 web 코어 동거 도메인이라 직접 쓰기 유지(배포 경계 안)
                drawingBoardRepository.findBySurveyId(survey.id).orElseGet {
                    drawingBoardRepository.save(
                        DrawingBoard.create(
                            surveyId = survey.id,
                            boardSize = survey.rewardSetting.targetParticipantCount!!,
                            rewards = survey.rewardSetting.rewards,
                        ),
                    )
                }
                surveyRepository.save(survey.awaitPayment())
                CheckoutPrep(
                    paymentRequired = true,
                    amount = rewardUnitPrice * survey.rewardSetting.rewards.sumOf { it.count },
                )
            }

            // 결제 대기 중 재호출 - 설문을 열지 않고(!) 기존 주문으로 안내.
            // 주문이 없으면(직전 발급 실패) amount 를 들려 보내 tx 밖에서 재발급한다(자기치유).
            survey.status == SurveyStatus.PENDING_PAYMENT -> {
                val rewardSetting = survey.rewardSetting
                CheckoutPrep(
                    paymentRequired = true,
                    amount =
                        if (rewardSetting is ImmediateDrawSetting) {
                            rewardUnitPrice * rewardSetting.rewards.sumOf { it.count }
                        } else {
                            0
                        },
                    existingOrderId =
                        paymentOrderRepository
                            .findBySurveyId(survey.id)
                            .map { it.tossOrderId }
                            .orElse(null),
                )
            }

            // 결제가 필요 없는 경우(수정 재개 등) - 기존 즉시 시작
            else -> {
                surveyRepository.save(survey.start())
                CheckoutPrep(paymentRequired = false)
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
