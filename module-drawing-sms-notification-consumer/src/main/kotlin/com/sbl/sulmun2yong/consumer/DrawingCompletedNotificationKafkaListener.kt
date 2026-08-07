package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedNotificationConsumedEvent
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.global.kafka.consumer.replay.ReplayableConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

// drawing-completed 토픽을 drawing-notification groupId로 구독하는 Kafka 어댑터.
// 책임: payload 역직렬화 → ApplicationEvent 발행 → Ack 위임. 도메인 분기/저장은 도메인 listener가 수행한다.
// ConsumerSeekAware로 파티션 콜백을 보존하여 처음/특정 시점/특정 offset부터 임의 리플레이가 가능하다.
// 리플레이 시 SMS 잡은 UNIQUE(event_id, notification_type)로 중복이 흡수된다.
@Component
class DrawingCompletedNotificationKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ConsumerSeekAware,
    ReplayableConsumer {
    companion object {
        const val LISTENER_ID = "drawing-notification-listener"
        const val TOPIC = "drawing-completed"
        private val log = LoggerFactory.getLogger(DrawingCompletedNotificationKafkaListener::class.java)
    }

    // ReplayableConsumer 식별자 — actuator endpoint의 target 매칭 키
    override val name: String = LISTENER_ID

    // 현재 컨슈머에 할당된 파티션별 ConsumerSeekCallback — 리플레이 시 seek 호출에 사용
    private val seekCallbacks = ConcurrentHashMap<TopicPartition, ConsumerSeekAware.ConsumerSeekCallback>()

    @KafkaListener(
        id = LISTENER_ID,
        topics = [TOPIC],
        groupId = "drawing-notification",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)
        // drawing-completed 에는 상류(backend) 상관 헤더가 없으므로 payload 의 사가 키로 시드한다.
        MDC.put("correlationId", event.eventId)
        applicationEventPublisher.publishEvent(
            DrawingCompletedNotificationConsumedEvent(
                eventId = event.eventId,
                surveyId = event.surveyId,
                isWinner = event.isWinner,
                rawPayload = payload,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        assignments.keys.forEach { seekCallbacks[it] = callback }
        log.info("drawing-notification listener 파티션 할당: {}", assignments.keys)
    }

    override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
        partitions.forEach { seekCallbacks.remove(it) }
    }

    // 현재 컨슈머에 할당된 모든 파티션을 토픽 시작 offset으로 되돌린다.
    override fun fromBeginning() {
        val partitions = seekCallbacks.keys.toList()
        if (partitions.isEmpty()) {
            log.warn("drawing-notification listener에 할당된 파티션이 없어 리플레이를 건너뜁니다.")
            return
        }
        partitions.forEach { tp ->
            seekCallbacks[tp]?.seekToBeginning(tp.topic(), tp.partition())
        }
        log.info("drawing-notification 리플레이: 시작 offset로 seek 완료, partitions={}", partitions)
    }

    // 특정 epoch millisecond 이후 메시지부터 다시 처리한다.
    override fun fromTimestamp(epochMillis: Long) {
        val partitions = seekCallbacks.keys.toList()
        if (partitions.isEmpty()) {
            log.warn("drawing-notification listener에 할당된 파티션이 없어 리플레이를 건너뜁니다.")
            return
        }
        partitions.forEach { tp ->
            seekCallbacks[tp]?.seekToTimestamp(tp.topic(), tp.partition(), epochMillis)
        }
        log.info("drawing-notification 리플레이: timestamp={} 부터 seek 완료, partitions={}", epochMillis, partitions)
    }

    // 단일 파티션의 특정 offset부터 다시 처리한다.
    override fun fromOffset(
        partition: Int,
        offset: Long,
    ) {
        val tp = TopicPartition(TOPIC, partition)
        val cb =
            seekCallbacks[tp]
                ?: run {
                    log.warn("drawing-notification listener에 partition={} 콜백이 없어 리플레이를 건너뜁니다.", partition)
                    return
                }
        cb.seek(TOPIC, partition, offset)
        log.info("drawing-notification 리플레이: partition={}, offset={} 로 seek 완료", partition, offset)
    }
}
