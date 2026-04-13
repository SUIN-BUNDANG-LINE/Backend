package com.sbl.sulmun2yong.notification.service

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class LoggingSmsSender(
    @Value("\${sms.mock.failure-rate:0.0}")
    private val failureRate: Double,
    @Value("\${sms.mock.delay-ms:0}")
    private val delayMs: Long,
) : SmsSender {
    companion object {
        private val log = LoggerFactory.getLogger(LoggingSmsSender::class.java)
    }

    override fun sendWinnerNotification(event: DrawingCompletedEvent) {
        // TODO 1 (지연):
        if (delayMs > 0) {
            Thread.sleep(delayMs)
        }

        // TODO 2 (실패):
        if (Random.nextDouble() < failureRate) {
            throw RuntimeException("[SMS MOCK] 시뮬레이션 실패: eventId=${event.eventId}")
        }

        //  TODO 3 (로그):
        log.info(
            "[SMS MOCK] 당첨자 알림 발송 성공 — eventId={}, participantId={}, rewardName={}",
            event.eventId,
            event.participantId,
            event.rewardName,
        )
    }
}
