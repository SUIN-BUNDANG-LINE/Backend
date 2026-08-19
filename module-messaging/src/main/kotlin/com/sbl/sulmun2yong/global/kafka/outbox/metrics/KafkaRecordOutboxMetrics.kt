package com.sbl.sulmun2yong.global.kafka.outbox.metrics

import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.KafkaRecordOutboxRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class KafkaRecordOutboxMetrics(
    private val registry: MeterRegistry,
    private val repository: KafkaRecordOutboxRepository,
) {
    private val pendingCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)

    init {
        registerGauges()
    }

    private fun registerGauges() {
        Gauge
            .builder("kafka_record_outbox_pending", pendingCount) { it.get().toDouble() }
            .register(registry)

        Gauge
            .builder("kafka_record_outbox_failed", failedCount) { it.get().toDouble() }
            .register(registry)
    }

    @Scheduled(fixedDelay = 30_000)
    fun refreshGauges() {
        pendingCount.set(repository.countByStatus(KafkaRecordOutboxStatus.PENDING))
        failedCount.set(repository.countByStatus(KafkaRecordOutboxStatus.FAILED))
    }

    fun startSample(): Timer.Sample = Timer.start(registry)

    fun recordPublish(
        topic: String,
        status: String,
        sample: Timer.Sample,
    ) {
        Counter
            .builder("outbox_relay_publish_total")
            .tag("status", status)
            .tag("topic", topic)
            .register(registry)
            .increment()

        sample.stop(
            Timer
                .builder("outbox_relay_publish_duration_seconds")
                .tag("topic", topic)
                .register(registry),
        )
    }
}
