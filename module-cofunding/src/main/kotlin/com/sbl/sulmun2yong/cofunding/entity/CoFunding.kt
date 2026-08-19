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

    @Column(nullable = false)
    val capacity: Int,

    // 서로 계산해야될 금액
    @Column(nullable = false)
    var shareAmount: Int,

    @Column(nullable = false)
    val deadline: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CoFundingStatus = CoFundingStatus.PENDING,

) : BaseTimeEntity() {
    fun approve(totalAmount: Int) {
        if (status != CoFundingStatus.PENDING) throw InvalidCoFundingStateException()
        shareAmount = totalAmount / capacity
        status = CoFundingStatus.FUNDING
    }

    fun markRejected() {
        if (status != CoFundingStatus.PENDING) throw InvalidCoFundingStateException()
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
        fun create(
            surveyId: UUID,
            ownerId: UUID,
            capacity: Int,
            deadline: LocalDateTime,
        ): CoFunding {
            if (capacity < 1) throw InvalidCoFundingCapacityException()
            return CoFunding(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                ownerId = ownerId,
                capacity = capacity,
                shareAmount = 0,
                deadline = deadline,
            )
        }
    }
}
