package com.sbl.sulmun2yong.drawing.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class DrawingProcessMetrics(
    private val registry: MeterRegistry,
) {
    private val winnerCounter: Counter =
        Counter
            .builder("drawing_history_persisted_total")
            .tag("result", "winner")
            .register(registry)

    private val nonWinnerCounter: Counter =
        Counter
            .builder("drawing_history_persisted_total")
            .tag("result", "non_winner")
            .register(registry)

    fun recordPersisted(isWinner: Boolean) {
        if (isWinner) winnerCounter.increment() else nonWinnerCounter.increment()
    }
}
