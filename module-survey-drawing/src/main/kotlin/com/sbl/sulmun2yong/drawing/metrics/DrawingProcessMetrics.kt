package com.sbl.sulmun2yong.drawing.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

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

    // ────────────────────────────────────────────────────────────────────────
    // 통제 실험용 통일 지표 — 다섯 전략(mode) 이 **같은 메트릭**을 같은 방식으로 기록한다.
    // 전략마다 다른 이름의 지표를 보면 대조가 성립하지 않으므로, 판정 축은 아래 3종으로 통일한다.
    //   ① drawing_outcome_total{mode,outcome}   요청 단위 최종 결과 (성공률)
    //   ② drawing_duration_seconds{mode}        요청 단위 소요 시간 (지연) — 어디서 기다렸든 포함
    //   ③ drawing_attempt_total{mode,result}    시도 단위 (재시도 가시화) — 요청당 시도 횟수
    // ────────────────────────────────────────────────────────────────────────

    /** 요청 단위 결과 — outcome: success · deadlock · version_conflict · lock_timeout · rejected · other */
    fun recordOutcome(
        mode: String,
        outcome: String,
    ) {
        Counter
            .builder("drawing_outcome_total")
            .description("추첨 요청 최종 결과 (전략별 통일 지표)")
            .tag("mode", mode)
            .tag("outcome", outcome)
            .register(registry)
            .increment()
    }

    /** 요청 단위 소요 시간 — 경쟁 제어 대기(Redis 락·JVM 모니터·DB 락·재시도)를 모두 포함한다 */
    fun recordDuration(
        mode: String,
        nanos: Long,
    ) {
        Timer
            .builder("drawing_duration_seconds")
            .description("추첨 요청 소요 시간 (전략별 통일 지표)")
            .tag("mode", mode)
            .publishPercentileHistogram()
            .register(registry)
            .record(nanos, TimeUnit.NANOSECONDS)
    }

    /**
     * 임계구역 진입 대기 시간 — 경쟁 제어 장치 앞에서 기다린 시간.
     * 진입 제어가 없는 전략(NO_LOCK·SERIALIZABLE·OPTIMISTIC_RETRY)도 **0 을 기록**해
     * 다섯 전략이 항상 같은 시계열을 갖게 한다(값 없음과 0 을 구분하기 위함).
     */
    fun recordContentionWait(
        mode: String,
        nanos: Long,
    ) {
        Timer
            .builder("drawing_contention_wait_seconds")
            .description("임계구역 진입 대기 시간 (전략별 통일 지표, 진입 제어 없으면 0)")
            .tag("mode", mode)
            .publishPercentileHistogram()
            // 대시보드의 4단계 대기 분포용 고정 경계 — 백분위 히스토그램 버킷은 값이 유동적이라
            // le="0.01" 같은 질의가 맞지 않는다. 두 방식은 같은 타이머에 함께 등록된다.
            .serviceLevelObjectives(
                Duration.ofMillis(10),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(500),
            ).register(registry)
            .record(nanos, TimeUnit.NANOSECONDS)
    }

    /**
     * 임계구역 진입 결과 — result: success · failure(획득 실패).
     * 진입 제어가 없는 전략도 success 를 기록해 시계열을 맞춘다.
     */
    fun recordEntry(
        mode: String,
        result: String,
    ) {
        Counter
            .builder("drawing_entry_total")
            .description("임계구역 진입 시도 결과 (전략별 통일 지표)")
            .tag("mode", mode)
            .tag("result", result)
            .register(registry)
            .increment()
    }

    /** 시도 단위 — result: success · version_conflict · stale_row · deadlock · other. 재시도가 없는 전략은 요청당 1회 */
    fun recordAttempt(
        mode: String,
        result: String,
    ) {
        Counter
            .builder("drawing_attempt_total")
            .description("추첨 시도 (재시도 포함, 전략별 통일 지표)")
            .tag("mode", mode)
            .tag("result", result)
            .register(registry)
            .increment()
    }

}
