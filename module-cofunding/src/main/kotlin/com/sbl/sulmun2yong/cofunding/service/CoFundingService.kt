package com.sbl.sulmun2yong.cofunding.service

import com.sbl.sulmun2yong.cofunding.dto.request.CoFundingStartRequest
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingMyOrderResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStartResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStatusResponse
import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingRequestException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingStateException
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingSagaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

// 공동 모금 개시 & 결제 진입 - 사가의 출발점.
// 단일 기록자 + 무교차: 개설 tx 는 co_fundings·participants 만 쓰고 읽기도 자기 테이블뿐이다.
// 설문 검증·총액 확정은 데이터 소유자(설문)의 판정 리스너 몫 - co-funding-requested 로 위임하고
// approved/rejected 회신으로 수렴한다. 응답은 "접수(PENDING_APPROVAL)"까지 보증한다.
@Service
class CoFundingService(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val coFundingSagaPublisher: CoFundingSagaPublisher,
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) {
    companion object {
        private const val MAX_DEADLINE_DAYS = 7L
    }

    // 개설 접수 - 한 트랜잭션: 모금(PENDING_APPROVAL) + 참여자 명단 저장 + co-funding-requested Outbox 발행.
    // 설문 판정(소유자·상태·경품 설정·총액)은 승인 회신으로 수렴한다 - 프론트는 상태 조회로 확정을 기다린다.
    @Transactional
    fun start(
        surveyId: UUID,
        ownerId: UUID,
        request: CoFundingStartRequest,
    ): CoFundingStartResponse {
        validate(request, ownerId)

        if (coFundingRepository.findBySurveyId(surveyId) != null) {
            throw InvalidCoFundingStateException()
        }

        val funding =
            CoFunding.create(
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = request.participantUserIds.size + 1,
                deadline = request.deadline,
            )

        // 참여자 명단·주문 ID 확정 - 금액은 승인에서, 발급은 ② 이벤트의 결제 리스너 몫
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
        coFundingSagaPublisher.publishRequested(funding)

        return CoFundingStartResponse(
            fundingId = funding.id,
            status = funding.status,
            deadline = funding.deadline,
            inviteUrl = "$frontendBaseUrl/co-fundings/${funding.id}",
        )
    }

    // 상태 조회 - 접수 응답 이후 프론트가 판정 확정(FUNDING/REJECTED)을 폴링하는 진입점.
    // 분담금은 승인 전 0 - FUNDING 부터 유효하다.
    @Transactional(readOnly = true)
    fun findStatus(fundingId: UUID): CoFundingStatusResponse {
        val funding = coFundingRepository.findById(fundingId).orElseThrow { CoFundingNotFoundException() }
        return CoFundingStatusResponse(
            fundingId = funding.id,
            status = funding.status,
            sharedAmount = funding.shareAmount,
            ownerShareAmount = funding.ownerShareAmount,
            deadline = funding.deadline,
        )
    }

    // 내 주문 조회 - 전부 자기 데이터로 조립한다(orderId 는 개설 시 사전 발급, 금액은 승인에서 확정).
    // 승인 전(PENDING_APPROVAL·REJECTED)이면 결제 진입이 없으므로 404 - 프론트는 재시도한다.
    @Transactional(readOnly = true)
    fun findMyOrder(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingMyOrderResponse {
        val participant =
            coFundingParticipantRepository.findByFundingIdAndUserId(fundingId, userId)
                ?: throw CoFundingNotFoundException()
        val funding = coFundingRepository.findById(fundingId).orElseThrow { CoFundingNotFoundException() }
        if (funding.status == CoFundingStatus.PENDING_APPROVAL || funding.status == CoFundingStatus.REJECTED) {
            throw CoFundingNotFoundException()
        }
        return CoFundingMyOrderResponse(
            orderId = participant.tossOrderId,
            amount = if (participant.isOwner) funding.ownerShareAmount else funding.shareAmount,
            checkoutUrl = "/payments/checkout.html?orderId=${participant.tossOrderId}",
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
