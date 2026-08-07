package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Redis 분산락 — **운영 경로**. 경쟁 제어 지점은 **DB 앞단(Redis)**이다.
 *
 * 트랜잭션 시작 전에 설문 단위 상호배제를 확보하므로 경합이 MySQL 에 도달하지 않는다
 * (행 락 대기·데드락·롤백 0). 언락은 커밋이 끝난 뒤 수행해, 커밋 전에 다음 요청이 임계 구역에
 * 들어오는 일이 없다.
 *
 * 락을 `@RedissonLock` AOP 가 아니라 직접 제어하는 이유는 두 가지다:
 *  ① 진입 대기 시간을 통일 지표(drawing_contention_wait_seconds)로 **실측**해야 다섯 전략을 대조할 수 있다
 *  ② synchronized 전략과 동일한 구조(진입 → 트랜잭션 → 커밋 → 해제)가 되어 비교가 공정해진다
 */
@Component
class RedissonDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    private val redissonClient: RedissonClient,
    drawingBoardRepository: DrawingBoardRepository,
    drawingHistoryRepository: DrawingHistoryRepository,
    surveyRepository: SurveyRepository,
    encryptionUtils: EncryptionUtils,
    drawingProcessMetrics: DrawingProcessMetrics,
    drawingEventPublisher: DrawingEventPublisher,
) : AbstractDrawingStrategy(
        drawingBoardRepository,
        drawingHistoryRepository,
        surveyRepository,
        encryptionUtils,
        drawingProcessMetrics,
        drawingEventPublisher,
    ) {
    companion object {
        private const val WAIT_SECONDS = 5L // 락 획득 대기 한계 — 초과 시 429
        private const val LEASE_SECONDS = 10L // 자동 해제 시한 (보유자 장애 대비)
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override val mode = DrawMode.REDISSON

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val lock = redissonClient.getLock("drawingLock:$surveyId")
        val waitStart = System.nanoTime()
        val acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS)
        recordEntry(System.nanoTime() - waitStart, acquired)
        if (!acquired) throw TooManyLockRequestException()

        try {
            // 트랜잭션을 락 안에서 시작하고 커밋까지 마친 뒤 락을 놓는다
            return attempt {
                transactionTemplate.execute {
                    draw(surveyId, participantId, selectedNumber, phoneNumber)
                }!!
            }
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }
}
