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

    // 낙관락 재시도 실험용 — 성공당 시도 횟수(attempts-per-success) 계측.
    // result=success(성공한 시도) / version_conflict(버전 충돌) / deadlock(데드락)으로 버려진 시도
    private val attemptSuccessCounter: Counter =
        Counter
            .builder("drawing_attempts_total")
            .tag("result", "success")
            .register(registry)

    private val attemptVersionConflictCounter: Counter =
        Counter
            .builder("drawing_attempts_total")
            .tag("result", "version_conflict")
            .register(registry)

    private val attemptDeadlockCounter: Counter =
        Counter
            .builder("drawing_attempts_total")
            .tag("result", "deadlock")
            .register(registry)

    fun recordAttemptSuccess() = attemptSuccessCounter.increment()

    fun recordAttemptVersionConflict() = attemptVersionConflictCounter.increment()

    fun recordAttemptDeadlock() = attemptDeadlockCounter.increment()
}
