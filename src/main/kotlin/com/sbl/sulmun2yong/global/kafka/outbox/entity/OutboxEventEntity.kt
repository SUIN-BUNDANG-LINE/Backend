package com.sbl.sulmun2yong.global.kafka.outbox.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_events")
class OutboxEventEntity(
    // 엔티티 고유 식별자. UUID를 앱에서 생성하여 DB INSERT 전에 이벤트 페이로드의 eventId로 활용 가능
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    // 이벤트 발생 도메인 ("Drawing", "Survey"). 특정 도메인의 Outbox 이벤트를 조회할 때 사용
    @Column(nullable = false)
    val aggregateType: String,
    // 이벤트 발생 도메인 엔티티의 ID (surveyId 등). 특정 엔티티의 이벤트 이력 추적용
    @Column(nullable = false)
    val aggregateId: String,
    // 이벤트 종류 ("SurveyResponseSubmitted", "DrawingCompleted"). 범용성을 위해 String으로 둔다
    @Column(nullable = false)
    val eventType: String,
    // Kafka 발행 대상 토픽명
    @Column(nullable = false)
    val kafkaTopic: String,
    // Kafka 파티셔닝 키. 같은 키는 같은 파티션으로 들어가 순서 보장 (surveyId)
    @Column(nullable = false)
    val kafkaKey: String,
    // Kafka 메시지 본문. 이벤트 페이로드를 JSON 직렬화한 문자열
    @Column(nullable = false, columnDefinition = "TEXT")
    val kafkaPayload: String,
    // 이벤트 발행 상태. PENDING(대기) → PUBLISHED(발행 성공) 또는 FAILED(최종 실패)
    // PENDING 상태가 일정 시간 유지되면 Outbox Relay가 재발행 시도
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OutboxStatus = OutboxStatus.PENDING,
    // 이벤트 생성 시각. Relay가 재발행 대상을 조회할 때 createdAt 기준으로 필터링
    @Column(nullable = false)
    val createdAt: Instant,
    // Kafka 발행 성공 시각. PUBLISHED 마킹 시 기록
    var publishedAt: Instant? = null,
) {
    fun markPublished() {
        this.status = OutboxStatus.PUBLISHED
        this.publishedAt = Instant.now()
    }

    fun markFailed() {
        this.status = OutboxStatus.FAILED
    }
}
