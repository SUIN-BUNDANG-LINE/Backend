package com.sbl.sulmun2yong.notification.service

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.notification.exception.SmsSendException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
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

    // 테스트용: -1이면 프로퍼티 기반, 0이면 항상 성공, N이면 N회까지 실패
    @Volatile
    var forcedFailCount: Int = -1

    private val callCount = AtomicInteger(0)

    override fun sendWinnerNotification(event: DrawingCompletedEvent) {
        if (delayMs > 0) {
            Thread.sleep(delayMs)
        }

        val count = callCount.incrementAndGet()

        // 강제 실패 모드 (테스트용)
        if (forcedFailCount > 0 && count <= forcedFailCount) {
            throw SmsSendException("[SMS MOCK] 강제 실패 ($count/$forcedFailCount): eventId=${event.eventId}")
        }
        if (forcedFailCount == 0) {
            // 항상 실패
            throw SmsSendException("[SMS MOCK] 항상 실패: eventId=${event.eventId}")
        }

        // 프로퍼티 기반 실패 (-1일 때)
        if (forcedFailCount == -1 && Random.nextDouble() < failureRate) {
            throw SmsSendException("[SMS MOCK] 시뮬레이션 실패: eventId=${event.eventId}")
        }

        log.info(
            "[SMS MOCK] 당첨자 알림 발송 성공 — eventId={}, participantId={}, rewardName={}",
            event.eventId,
            event.participantId,
            event.rewardName,
        )
    }

    fun resetTestState() {
        forcedFailCount = -1
        callCount.set(0)
    }
}
