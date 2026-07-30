package com.sbl.sulmun2yong.cofunding.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

// co_fundings 슬림 사본 - 컨슈머가 만지는 컬럼만 매핑한다 (원본: modules-web :support).
// 쓰기는 전부 리포지토리의 조건부 UPDATE(CAS)라 전이 메서드가 없다.
@Entity
@Table(name = "co_fundings")
class CoFunding(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,

    // 장벽 판정 분모 (tryConfirm 서브쿼리와 비교)
    @Column
    val capacity: Int,

    // 기한 만료 스캔 기준 (기한 스케쥴러)
    @Column(nullable = false)
    val deadline: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: CoFundingStatus,

    // CAS 가 :now 로 직접 세팅 - 감사(auditing) 미사용
    val updatedAt: LocalDateTime,
)
