package com.sbl.sulmun2yong.global.kafka.outbox.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "kafka_record_outbox")
class KafkaRecordOutboxEntity(
    // 엔티티 고유 식별자 = 페이로드의 eventId. 앱이 생성해 INSERT 전에 알 수 있으므로,
    // 컨슈머가 남긴 eventId 로 이 행을 PK 조회 한 번에 되짚을 수 있다.
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    // Kafka 발행 대상 토픽명
    @Column(nullable = false)
    val kafkaTopic: String,

    // Kafka 파티셔닝 키. 같은 키는 같은 파티션으로 들어가 순서 보장
    @Column(nullable = false)
    val kafkaRecordKey: String,

    // Kafka 메시지 본문. 이벤트 페이로드를 JSON 직렬화한 문자열
    @Column(nullable = false, columnDefinition = "TEXT")
    val kafkaRecordValue: String,

    // 이벤트 발행 상태. PENDING(대기) → PUBLISHED(발행 성공) 또는 FAILED(최종 실패)
    // PENDING 상태가 일정 시간 유지되면 Outbox Relay가 재발행 시도
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: KafkaRecordOutboxStatus = KafkaRecordOutboxStatus.PENDING,

    // 이벤트 생성 시각. Relay가 재발행 대상을 조회할 때 createdAt 기준으로 필터링
    @Column(nullable = false)
    val createdAt: Instant,

    // Kafka 발행 성공 시각. PUBLISHED 마킹 시 기록
    var publishedAt: Instant? = null,
    @Column(nullable = false)
    var retryCount: Int = 0,
) {

    companion object {
        const val MAX_RETRY_COUNT = 5

        fun create(
            eventId: UUID,
            kafkaTopic: String,
            kafkaRecordKey: String,
            kafkaRecordValue: String,
        ) = KafkaRecordOutboxEntity(
            id = eventId,
            kafkaTopic = kafkaTopic,
            kafkaRecordKey = kafkaRecordKey,
            kafkaRecordValue = kafkaRecordValue,
            createdAt = Instant.now(),
        )
    }

    fun markPublished() {
        this.status = KafkaRecordOutboxStatus.PUBLISHED
        this.publishedAt = Instant.now()
    }

    fun incrementRetry(): Boolean {
        retryCount++
        if (retryCount >= MAX_RETRY_COUNT) {
            this.status = KafkaRecordOutboxStatus.FAILED
            return true
        }
        return false
    }

}
