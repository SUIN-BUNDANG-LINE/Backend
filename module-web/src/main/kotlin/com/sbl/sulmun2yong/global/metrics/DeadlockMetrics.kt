package com.sbl.sulmun2yong.global.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DeadlockMetrics(
    registry: MeterRegistry,
) {
    private val deadlockCounter: Counter =
        Counter
            .builder("db_deadlock_total")
            .description("MySQL InnoDB deadlock 검출 카운트 (CannotAcquireLockException 발생 시 +1)")
            .register(registry)

    fun recordDeadlock() {
        deadlockCounter.increment()
    }
}
