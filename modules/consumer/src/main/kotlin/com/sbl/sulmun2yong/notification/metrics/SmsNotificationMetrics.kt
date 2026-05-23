package com.sbl.sulmun2yong.notification.metrics

import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class SmsNotificationMetrics(
    private val registry: MeterRegistry,
    private val jobRepository: SmsNotificationJobRepository,
    private val dltRepository: DltMessageRepository,
) {
    private val dltCounts = ConcurrentHashMap<String, AtomicLong>()
    private val attemptCounters = ConcurrentHashMap<String, Counter>()
    private val statusCounts = SmsJobStatus.entries.associateWith { AtomicLong(0) }

    init {
        statusCounts.forEach { (status, atom) ->
            Gauge
                .builder("sms_notification_jobs_count", atom) { it.get().toDouble() }
                .tag("status", status.name)
                .register(registry)
        }
    }

    @Scheduled(fixedDelay = 30_000)
    fun refreshGauges() {
        statusCounts.forEach { (status, atom) ->
            atom.set(jobRepository.countByStatus(status))
        }
        dltCounts.values.forEach { it.set(0) }
        dltRepository.countGroupByNotificationType().forEach { row ->
            dltCounts
                .computeIfAbsent(row.getNotificationType()) { type ->
                    val atom = AtomicLong(0)
                    Gauge
                        .builder("dlt_messages_total", atom) { it.get().toDouble() }
                        .tag("notification_type", type)
                        .register(registry)
                    atom
                }.set(row.getCount())
        }
    }

    fun recordAttempt(result: String) {
        attemptCounters
            .computeIfAbsent(result) {
                Counter
                    .builder("sms_notification_attempts_total")
                    .tag("result", it)
                    .register(registry)
            }.increment()
    }
}
