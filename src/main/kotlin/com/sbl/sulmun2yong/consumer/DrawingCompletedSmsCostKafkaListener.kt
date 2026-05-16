package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedSmsCostConsumedEvent
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

// drawing-completed 토픽을 sms-cost-calculator groupId로 fan-out 구독하는 Kafka 어댑터.
// ConsumerSeekAware로 파티션 콜백을 보존하여 처음/특정 시점/특정 offset부터 임의 리플레이가 가능하다.
@Component
class DrawingCompletedSmsCostKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ConsumerSeekAware {
    companion object {
        const val LISTENER_ID = "sms-cost-calculator-listener"
        const val TOPIC = "drawing-completed"
        private val log = LoggerFactory.getLogger(DrawingCompletedSmsCostKafkaListener::class.java)
    }

    // 현재 컨슈머에 할당된 파티션별 ConsumerSeekCallback — 리플레이 시 seek 호출에 사용
    private val seekCallbacks = ConcurrentHashMap<TopicPartition, ConsumerSeekAware.ConsumerSeekCallback>()

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        assignments.keys.forEach { seekCallbacks[it] = callback }
        log.info("sms-cost listener 파티션 할당: {}", assignments.keys)
    }

    override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
        partitions.forEach { seekCallbacks.remove(it) }
    }

    @KafkaListener(
        id = LISTENER_ID,
        topics = [TOPIC],
        groupId = "sms-cost-calculator",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)
        applicationEventPublisher.publishEvent(
            DrawingCompletedSmsCostConsumedEvent(
                eventId = event.eventId,
                surveyId = event.surveyId,
                isWinner = event.isWinner,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }

    // 현재 컨슈머에 할당된 모든 파티션을 토픽 시작 offset으로 되돌린다.
    fun replayFromBeginning() {
        val partitions = seekCallbacks.keys.toList()
        if (partitions.isEmpty()) {
            log.warn("sms-cost listener에 할당된 파티션이 없어 리플레이를 건너뜁니다.")
            return
        }
        partitions.forEach { tp ->
            seekCallbacks[tp]?.seekToBeginning(tp.topic(), tp.partition())
        }
        log.info("sms-cost 리플레이: 시작 offset로 seek 완료, partitions={}", partitions)
    }

    // 특정 epoch millisecond 이후 메시지부터 다시 처리한다.
    fun replayFromTimestamp(epochMillis: Long) {
        val partitions = seekCallbacks.keys.toList()
        if (partitions.isEmpty()) {
            log.warn("sms-cost listener에 할당된 파티션이 없어 리플레이를 건너뜁니다.")
            return
        }
        partitions.forEach { tp ->
            seekCallbacks[tp]?.seekToTimestamp(tp.topic(), tp.partition(), epochMillis)
        }
        log.info("sms-cost 리플레이: timestamp={} 부터 seek 완료, partitions={}", epochMillis, partitions)
    }

    // 단일 파티션의 특정 offset부터 다시 처리한다.
    fun replayFromOffset(
        partition: Int,
        offset: Long,
    ) {
        val tp = TopicPartition(TOPIC, partition)
        val cb =
            seekCallbacks[tp]
                ?: run {
                    log.warn("sms-cost listener에 partition={} 콜백이 없어 리플레이를 건너뜁니다.", partition)
                    return
                }
        cb.seek(TOPIC, partition, offset)
        log.info("sms-cost 리플레이: partition={}, offset={} 로 seek 완료", partition, offset)
    }
}
