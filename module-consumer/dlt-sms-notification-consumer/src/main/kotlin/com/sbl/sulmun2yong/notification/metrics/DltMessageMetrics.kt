package com.sbl.sulmun2yong.notification.metrics

import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// DLT 영속화 모듈(:dlt-sms-notification-consumer)에 동거하는 DLT 누적 메트릭.
// 기존엔 :drawing-sms-notification-consumer 의 SmsNotificationMetrics 가 함께 노출했으나,
// 모듈 분리 후 DltMessageRepository 가 이 모듈에만 존재하므로 측정 위치도 함께 이동했다.
@Component
class DltMessageMetrics(
    private val registry: MeterRegistry,
    private val dltRepository: DltMessageRepository,
) {
    private val dltCounts = ConcurrentHashMap<String, AtomicLong>()

    @Scheduled(fixedDelay = 30_000)
    fun refreshGauges() {
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
}
