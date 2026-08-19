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

    /**
     * 요청 단위 결과 — outcome: success · duplicate_ticket · deadlock ·
     * lock_timeout · rejected · other
     */
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
     * 진입 제어가 없는 전략(DEFAULT·SERIALIZABLE)도 **0 을 기록**해
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

}
