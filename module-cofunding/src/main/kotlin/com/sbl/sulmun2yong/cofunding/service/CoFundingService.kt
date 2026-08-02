package com.sbl.sulmun2yong.cofunding.service

import com.sbl.sulmun2yong.cofunding.dto.request.CoFundingStartRequest
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingMyOrderResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStartResponse
import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingRequestException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingStateException
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingSagaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
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
// 단일 기록자: 개설 tx 는 co_fundings·participants 만 쓴다. 주문 발급(결제)·설문 대기 전이(설문)·
// 보드 생성(추첨)은 co-funding-created 이벤트의 구독자가 각자 자기 테이블에 수행한다.
// 설문 상태 검증은 교차 "읽기"(공유 DB 허용) - 쓰기가 아니다.
@Service
class CoFundingService(
    private val surveyRepository: SurveyRepository,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val coFundingSagaPublisher: CoFundingSagaPublisher,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) {
    companion object {
        private const val MAX_DEADLINE_DAYS = 7L
    }

    // 개시 - 한 트랜잭션: 모금 + 참여자 명단 저장 + co-funding-created Outbox 발행.
    // 응답이 보증하는 것은 "모금 생성"까지 - 주문·설문 전이는 이벤트로 수렴한다(최종적 일관성).
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

        val totalAmount = rewardUnitPrice * survey.rewardSetting.rewards.sumOf { it.count }
        val funding =
            CoFunding.create(
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = request.participantUserIds.size + 1,
                totalAmount = totalAmount,
                deadline = request.deadline,
            )

        // 참여자 명단 확정 - 주문 금액은 role 별(개설자 = 분담금 + 잔액), 발급은 결제 리스너 몫
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

        coFundingRepository.save(funding)
        coFundingParticipantRepository.saveAll(participants)
        coFundingSagaPublisher.publishCreated(funding, participants)

        return CoFundingStartResponse(
            fundingId = funding.id,
            sharedAmount = funding.shareAmount,
            ownerShareAmount = funding.ownerShareAmount,
            deadline = funding.deadline,
            inviteUrl = "$frontendBaseUrl/co-fundings/${funding.id}",
        )
    }

    // 내 주문 조회 - 주문은 개설 이벤트의 결제 리스너가 발급하므로 잠깐 비어 있을 수 있다
    // (최종적 일관성 창). 아직 없으면 404 - 프론트는 재시도한다.
    @Transactional(readOnly = true)
    fun findMyOrder(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingMyOrderResponse {
        val participant =
            coFundingParticipantRepository.findByFundingIdAndUserId(fundingId, userId)
                ?: throw CoFundingNotFoundException()
        val order =
            paymentOrderRepository
                .findByTossOrderId(participant.tossOrderId)
                .orElseThrow { CoFundingNotFoundException() }
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
