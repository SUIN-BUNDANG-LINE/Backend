package com.sbl.sulmun2yong.survey.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

// surveys 슬림 사본 - 설문 활성화 CAS 전용. status 를 String 으로 매핑해
// 원본 SurveyStatus enum 전체를 복제하지 않는다 (쓰는 리터널은 두 개뿐)
@Entity
@Table(name = "surveys")
class Survey(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Column(nullable = false, length = 30)
    val status: String,

    @Column(nullable = false)
    val updatedAt: LocalDateTime,
)
