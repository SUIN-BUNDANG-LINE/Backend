package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.lock.RedissonLock
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Redis 분산락 — **운영 경로**. 경쟁 제어 지점은 **DB 앞단(Redis)**이다.
 *
 * `@RedissonLock` AOP 가 트랜잭션 시작 전에 설문 단위 상호배제를 확보하므로 경합이 MySQL 에
 * 도달하지 않는다(행 락 대기·데드락·롤백 0). 언락은 RedissonLockAspect 가 트랜잭션
 * 완료(afterCompletion) 후 수행해, 커밋 전에 다음 요청이 임계 구역에 들어오는 일이 없다.
 */
@Component
class RedissonDrawingStrategy(
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
    override val mode = DrawMode.REDISSON

    @RedissonLock(key = "drawingLock:{surveyId}", leaseTime = 10, waitTime = 5)
    @Transactional
    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = draw(surveyId, participantId, selectedNumber, phoneNumber)
}
