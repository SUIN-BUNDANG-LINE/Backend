package com.sbl.sulmun2yong.global.lock.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DrawingLockMetrics(
    private val registry: MeterRegistry,
) {
    fun startSample(): Timer.Sample = Timer.start(registry)

    fun recordAcquire(
        result: String,
        lockKeyType: String,
        sample: Timer.Sample,
    ) {
        Counter
            .builder("drawing_lock_acquire_total")
            .tag("result", result)
            .tag("lock_key_type", lockKeyType)
            .register(registry)
            .increment()

        sample.stop(
            Timer
                .builder("drawing_lock_wait_seconds")
                .tag("lock_key_type", lockKeyType)
                .serviceLevelObjectives(
                    Duration.ofMillis(10),
                    Duration.ofMillis(50),
                    Duration.ofMillis(100),
                    Duration.ofMillis(500),
                ).register(registry),
        )
    }
}
