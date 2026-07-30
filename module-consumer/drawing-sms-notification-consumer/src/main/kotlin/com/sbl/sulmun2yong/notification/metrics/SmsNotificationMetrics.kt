package com.sbl.sulmun2yong.notification.metrics

import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// SMS 발송 잡 메트릭(상태별 게이지, 시도 결과 카운터).
// DLT 누적 카운트는 :dlt-sms-notification-consumer 의 DltMessageMetrics 가 별도로 노출한다 — DLT 영속화를
// 이 모듈이 하지 않으므로 동일 컨슈머가 측정하면 jobs/attempts 와 DLT 카운트가 두 군데에서 부정확해진다.
@Component
class SmsNotificationMetrics(
    private val registry: MeterRegistry,
    private val jobRepository: SmsNotificationJobRepository,
) {
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
