package com.sbl.sulmun2yong.drawing.listener

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedSmsCostConsumedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// 당첨자(SMS 발송 대상) 1건당 단가를 누적해 SMS 비용을 산출한다.
// 리플레이로 동일 메시지가 다시 들어와도 누적되도록 의도된 설계 — 비용 재계산 시 reset() 후 리플레이.
@Component
class SmsCostEventListener {
    companion object {
        private val log = LoggerFactory.getLogger(SmsCostEventListener::class.java)

        // SMS 단건 단가(원) — 운영에서는 application.yml + @Value로 외부화 가능.
        private const val SMS_UNIT_COST_KRW = 8L
    }

    private val totalCostKrw = AtomicLong(0)
    private val sentCount = AtomicLong(0)
    private val costBySurvey = ConcurrentHashMap<String, AtomicLong>()

    @EventListener
    fun handle(event: DrawingCompletedSmsCostConsumedEvent) {
        if (!event.isWinner) return

        val cost = SMS_UNIT_COST_KRW
        val totalCost = totalCostKrw.addAndGet(cost)
        val totalSent = sentCount.incrementAndGet()
        val surveyCost =
            costBySurvey
                .computeIfAbsent(event.surveyId) { AtomicLong(0) }
                .addAndGet(cost)

        log.info(
            "SMS 비용 누적, eventId:{}, surveyId:{}, +{}원, 누적:{}원(설문기준 {}원), 발송수:{}",
            event.eventId,
            event.surveyId,
            cost,
            totalCost,
            surveyCost,
            totalSent,
        )
    }

    fun totalCostKrw(): Long = totalCostKrw.get()

    fun sentCount(): Long = sentCount.get()

    fun costBySurveyKrw(surveyId: String): Long = costBySurvey[surveyId]?.get() ?: 0L

    // 리플레이 전에 누적값을 0으로 초기화하기 위한 훅. (리플레이 컨트롤은 consumer/SmsCostReplayController에서 수행)
    fun reset() {
        totalCostKrw.set(0)
        sentCount.set(0)
        costBySurvey.clear()
        log.info("SMS 비용 카운터 리셋")
    }
}
