package com.sbl.sulmun2yong.cofunding.service

import com.sbl.sulmun2yong.cofunding.dto.request.CoFundingStartRequest
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingMyOrderResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStartResponse
import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingRequestException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingStateException
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

// 공동 모금 개시 & 결제 진입 - 사가의 출발점.
// 초대제(D7): 명단/분담금/주문이 개설 트랜잭션에서 전부 확정된다. 이후 이 모금에 쓰기는
// settle(참여자 SETTLED)/컨슈머 CAS/릴레이 후처리뿐이라 별도 수정 API가 없다.
@Service
class CoFundingService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) {
    companion object {
        private const val MAX_DEADLINE_DAYS = 7L
    }

    // 개시 - 한 트랜잭션: 모금 + 참여자 명단 + 참여자별 주문 사전 발급 + Survey 결제 대기 전이
    @Transactional
    fun start(
        surveyId: UUID,
        ownerId: UUID,
        request: CoFundingStartRequest,
    ): CoFundingStartResponse {
        validate(request, ownerId)

        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(surveyId, ownerId)
                .orElseThrow { SurveyNotFoundException() }

        // 경품 설문이 시작 전 상태일 때만 모금 개시 가능
        if (survey.rewardSetting !is ImmediateDrawSetting || survey.status != SurveyStatus.NOT_STARTED) {
            throw InvalidCoFundingStateException()
        }
        if (coFundingRepository.findBySurveyId(surveyId) != null) {
            throw InvalidCoFundingStateException()
        }

        // 경품 보드 - 기존 단독 결제 개시(startSurvey)와 동일하게 결제 대기 진입 시점에 준비
        drawingBoardRepository.findBySurveyId(survey.id).orElseGet {
            drawingBoardRepository.save(
                DrawingBoard.create(
                    surveyId = survey.id,
                    boardSize = survey.rewardSetting.targetParticipantCount!!,
                    rewards = survey.rewardSetting.rewards,
                ),
            )
        }

        val totalAmount = rewardUnitPrice * survey.rewardSetting.rewards.sumOf { it.count }
        val funding =
            CoFunding.create(
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = request.participantUserIds.size + 1,
                totalAmount = totalAmount,
                deadline = request.deadline,
            )

        // 참여자 + 주문 일괄 확정 - 주문 금액은 role 별(개설자 = 분담금 + 잔액)
        val owner = CoFundingParticipant.owner(funding.id, ownerId, newTossOrderId())
        val members =
            request.participantUserIds.map {
                CoFundingParticipant.member(
                    funding.id,
                    it,
                    newTossOrderId(),
                )
            }
        val participants = listOf(owner) + members
        val orders =
            participants.map { participant ->
                PaymentOrder.create(
                    surveyId = surveyId,
                    makerId = participant.userId,
                    orderId = participant.tossOrderId,
                    amount = if (participant.isOwner) funding.ownerShareAmount else funding.shareAmount,
                )
            }

        coFundingRepository.save(funding)
        coFundingParticipantRepository.saveAll(participants)
        paymentOrderRepository.saveAll(orders)
        surveyRepository.save(survey.awaitPayment())

        return CoFundingStartResponse(
            fundingId = funding.id,
            sharedAmount = funding.shareAmount,
            ownerShareAmount = funding.ownerShareAmount,
            deadline = funding.deadline,
            inviteUrl = "$frontendBaseUrl/co-fundings/${funding.id}",
        )
    }

    // 내 주문 조회 - 주문은 개설 때 사전 발급됐으므로 조회 전용 (멱등 발급 로직 없음, D7)
    @Transactional(readOnly = true)
    fun findMyOrder(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingMyOrderResponse {
        val participant =
            coFundingParticipantRepository.findByFundingIdAndUserId(fundingId, userId)
                ?: throw CoFundingNotFoundException()
        val order = paymentOrderRepository.findByTossOrderId(participant.tossOrderId).orElseThrow()
        return CoFundingMyOrderResponse(
            orderId = order.tossOrderId,
            amount = order.amount,
            checkoutUrl = "/payments/checkout.html?orderId=${order.tossOrderId}",
        )
    }

    private fun validate(
        request: CoFundingStartRequest,
        ownerId: UUID,
    ) {
        val members = request.participantUserIds
        if (members.toSet().size != members.size || ownerId in members) {
            throw InvalidCoFundingRequestException()
        }
        val now = LocalDateTime.now()
        if (request.deadline <= now || request.deadline > now.plusDays(MAX_DEADLINE_DAYS)) {
            throw InvalidCoFundingRequestException()
        }
    }

    private fun newTossOrderId() = "ord-${UUID.randomUUID()}"
}
