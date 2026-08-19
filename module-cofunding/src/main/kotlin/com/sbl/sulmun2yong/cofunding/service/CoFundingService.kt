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
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingOutboxKafkaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class CoFundingService(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val coFundingOutboxKafkaPublisher: CoFundingOutboxKafkaPublisher,
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) {
    companion object {
        private const val MAX_DEADLINE_DAYS = 7L
    }

    // 더치페이 시작
    @Transactional
    fun start(
        // 패스 파라미터
        surveyId: UUID,
        // HTTP 헤더에 있는 X-User-Id
        ownerId: UUID,
        // 요청 DTO
        request: CoFundingStartRequest,
    ): CoFundingStartResponse {
        // 개최자 ID가 요청 DTO에 있는지 확인
        if (ownerId !in request.participantUserIds) {
            throw InvalidCoFundingRequestException()
        }
        // 데드라인이 허용범위 내인지 검증
        val now = LocalDateTime.now()
        if (request.deadline <= now || request.deadline > now.plusDays(MAX_DEADLINE_DAYS)) {
            throw InvalidCoFundingRequestException()
        }
        // 진행 중인 모금이 있으면 중복 접수 차단 - 종착(거절·무산·환불) 건은 재접수 허용
        val activeStatuses = listOf(CoFundingStatus.PENDING, CoFundingStatus.FUNDING, CoFundingStatus.CONFIRMED)
        if (coFundingRepository.existsBySurveyIdAndStatusIn(surveyId, activeStatuses)) {
            throw InvalidCoFundingStateException()
        }

        // 더치페이 주문 정보 엔티티 생성
        val funding =
            CoFunding.create(
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = request.participantUserIds.size,
                deadline = request.deadline,
            )

        // 참가자 엔티티를 만든다 - 각자 따로 결제하므로 주문번호도 참여자마다 발급한다
        val participants =
            request.participantUserIds.map { userId ->
                CoFundingParticipant.of(funding.id, userId, "ord-${UUID.randomUUID()}")
            }

        // 영속성 컨텍스트에 등록한다
        coFundingRepository.save(funding)
        // 영속성 컨텍스트에 등록한다
        coFundingParticipantRepository.saveAll(participants)
        // 아웃박스 이벤트를 만들고 -> 카프카 브로커에 레코드를 전송한다
        coFundingOutboxKafkaPublisher.publishRequested(funding)

        // 접수증만 응답한다 - 좌표(fundingId)와 접수 사실. 나머지는 폴링(상태 조회)의 몫.
        return CoFundingStartResponse(
            fundingId = funding.id,
            status = funding.status,
        )
    }

    @Transactional(readOnly = true)
    fun findStatus(fundingId: UUID): CoFundingStatusResponse {
        val funding =
            coFundingRepository.findById(fundingId).orElseThrow { CoFundingNotFoundException() }
        // 초대 링크는 확정 후에만 - my-order 의 승인 전 404 와 같은 노출 게이트
        val inviteUrl =
            if (funding.status == CoFundingStatus.PENDING || funding.status == CoFundingStatus.REJECTED) {
                null
            } else {
                "$frontendBaseUrl/co-fundings/${funding.id}"
            }
        return CoFundingStatusResponse(
            fundingId = funding.id,
            status = funding.status,
            sharedAmount = funding.shareAmount,
            deadline = funding.deadline,
            inviteUrl = inviteUrl,
        )
    }

    @Transactional(readOnly = true)
    fun findMyOrder(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingMyOrderResponse {
        val participant =
            coFundingParticipantRepository.findByFundingIdAndUserId(fundingId, userId)
                ?: throw CoFundingNotFoundException()
        val funding =
            coFundingRepository.findById(fundingId).orElseThrow { CoFundingNotFoundException() }
        if (funding.status == CoFundingStatus.PENDING || funding.status == CoFundingStatus.REJECTED) {
            throw CoFundingNotFoundException()
        }
        return CoFundingMyOrderResponse(
            orderId = participant.tossOrderId,
            amount = funding.shareAmount,
            checkoutUrl = "/payments/checkout.html?orderId=${participant.tossOrderId}",
        )
    }
}
