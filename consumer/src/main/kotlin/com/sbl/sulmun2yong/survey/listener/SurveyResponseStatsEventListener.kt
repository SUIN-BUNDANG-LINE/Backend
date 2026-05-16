package com.sbl.sulmun2yong.survey.listener

import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedStatsConsumedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// survey-response-submitted 토픽을 별도 groupId(response-stats)로 fan-out 구독하여
// 응답 현황을 인메모리 카운터로 집계한다. (PRD F006 — 실시간 통계 Consumer 부분 구현)
// 동일 토픽에 다른 groupId로 추가된 컨슈머이므로 자동 마감 로직과 독립적으로 동작한다.
@Component
class SurveyResponseStatsEventListener {
    companion object {
        private val log = LoggerFactory.getLogger(SurveyResponseStatsEventListener::class.java)
    }

    // 설문별 누적 응답 수 (인메모리 — 운영에서는 Redis/메트릭 백엔드로 대체 가능)
    private val responseCountBySurvey = ConcurrentHashMap<String, AtomicLong>()

    @EventListener
    fun handle(event: SurveyResponseSubmittedStatsConsumedEvent) {
        val accumulated =
            responseCountBySurvey
                .computeIfAbsent(event.surveyId) { AtomicLong(0) }
                .incrementAndGet()

        log.info(
            "응답 통계 갱신, surveyId:{}, currentParticipantCount:{}, accumulated:{}",
            event.surveyId,
            event.currentParticipantCount,
            accumulated,
        )
    }

    fun snapshot(surveyId: String): Long = responseCountBySurvey[surveyId]?.get() ?: 0L
}
