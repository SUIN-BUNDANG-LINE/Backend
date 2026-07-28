package com.sbl.sulmun2yong.global.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class OptimisticLockMetrics(
    registry: MeterRegistry,
) {
    private val conflictCounter: Counter =
        Counter
            .builder("optimistic_lock_failure_total")
            .description("낙관적 락 버전 충돌 카운트 (ObjectOptimisticLockingFailureException 발생 시 +1)")
            .register(registry)

    fun recordConflict() {
        conflictCounter.increment()
    }
}
