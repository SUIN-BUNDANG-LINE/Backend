package com.sbl.sulmun2yong.global.kafka.outbox.metrics

import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class OutboxMetrics(
    private val registry: MeterRegistry,
    private val repository: OutboxEventRepository,
) {
    private val pendingDrawingCount = AtomicLong(0)
    private val pendingSurveyCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)

    init {
        registerGauges()
    }

    private fun registerGauges() {
        Gauge
            .builder("outbox_events_pending", pendingDrawingCount) { it.get().toDouble() }
            .tag("aggregate_type", "Drawing")
            .register(registry)

        Gauge
            .builder("outbox_events_pending", pendingSurveyCount) { it.get().toDouble() }
            .tag("aggregate_type", "Survey")
            .register(registry)

        Gauge
            .builder("outbox_events_failed", failedCount) { it.get().toDouble() }
            .register(registry)
    }

    @Scheduled(fixedDelay = 30_000)
    fun refreshGauges() {
        pendingDrawingCount.set(
            repository.countByStatusAndAggregateType(OutboxStatus.PENDING, "Drawing"),
        )
        pendingSurveyCount.set(
            repository.countByStatusAndAggregateType(OutboxStatus.PENDING, "Survey"),
        )
        failedCount.set(repository.countByStatus(OutboxStatus.FAILED))
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
