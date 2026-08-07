package com.sbl.sulmun2yong.cofunding.entity

import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingCapacityException
import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingStateException
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

// 공동 모금 - 설문당 1건, 사가 상태 머신(FUNDING->CONFIRMED/FAILED->REFUNDED)의 단일 행.
// 경합 구간(장벽&무산&정원)의 전이는 리포지토리의 조건부 UPDATE(CAS)가 수행한다.
// 이 엔티티의 전이 메서드는 이미 행을 점유한 트랜잭션 & 테스트용 가드다.
@Entity
@Table(name = "co_fundings")
class CoFunding(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val ownerId: UUID,
    // 공동 주최 정원(개설자 포함, 2 이상)
    @Column(nullable = false)
    val capacity: Int,
    // 참여자 1인 분담금(원) - 접수 시점엔 0, 설문 승인(approve)에서 확정된다
    @Column(nullable = false)
    var shareAmount: Int,
    // 개설자 분담금 = 분담금 + 균등 분할 잔액 - 접수 시점엔 0, 승인에서 확정
    @Column(nullable = false)
    var ownerShareAmount: Int,
    @Column(nullable = false)
    val deadline: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CoFundingStatus = CoFundingStatus.PENDING_APPROVAL,

) : BaseTimeEntity() {
    fun isExpired(now: LocalDateTime): Boolean = now.isAfter(deadline)

    // 설문 판정 승인 - 총액을 받아 분담금을 확정하고 모금을 연다.
    // findByIdForUpdate 행 잠금 아래에서 호출된다 - PENDING_APPROVAL 가드가 재전달 멱등을 보장.
    fun approve(totalAmount: Int) {
        if (status != CoFundingStatus.PENDING_APPROVAL) throw InvalidCoFundingStateException()
        shareAmount = totalAmount / capacity
        ownerShareAmount = shareAmount + totalAmount % capacity
        status = CoFundingStatus.FUNDING
    }

    // 설문 판정 거절 - 결제자가 없는 접수 상태의 종착.
    fun markRejected() {
        if (status != CoFundingStatus.PENDING_APPROVAL) throw InvalidCoFundingStateException()
        status = CoFundingStatus.REJECTED
    }

    fun markConfirmed() {
        if (status != CoFundingStatus.FUNDING) throw InvalidCoFundingStateException()
        status = CoFundingStatus.CONFIRMED
    }

    fun markFailed() {
        if (status != CoFundingStatus.FUNDING) throw InvalidCoFundingStateException()
        status = CoFundingStatus.FAILED
    }

    fun markRefunded() {
        if (status != CoFundingStatus.FAILED) throw InvalidCoFundingStateException()
        status = CoFundingStatus.REFUNDED
    }

    companion object {
        // 개설 접수 - 금액은 설문 승인(approve)에서 확정되므로 0 으로 시작한다.
        // 모금은 설문을 읽지 않는다 - 총액 재료(경품 수·단가)는 설문 소유 데이터다.
        fun create(
            surveyId: UUID,
            ownerId: UUID,
            capacity: Int,
            deadline: LocalDateTime,
        ): CoFunding {
            if (capacity < 2) throw InvalidCoFundingCapacityException()
            return CoFunding(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = capacity,
                shareAmount = 0,
                ownerShareAmount = 0,
                deadline = deadline,
            )
        }
    }
}
