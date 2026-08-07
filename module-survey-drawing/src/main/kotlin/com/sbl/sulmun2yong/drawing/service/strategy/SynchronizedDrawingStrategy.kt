package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM 로컬 직렬화 (비교 실험 전용) — 경쟁 제어 지점은 **JVM 로컬 모니터**다.
 *
 * 모니터가 인스턴스(JVM)마다 별개라 같은 JVM 안에서는 완벽히 직렬화되지만 cross-JVM 경합은
 * 그대로 통과한다 — 그 한계를 실측하는 것이 이 경로의 목적이다.
 *
 * 트랜잭션은 `TransactionTemplate` 으로 **락 안에서** 열고 닫는다. `@Transactional` 을 쓰면
 * 트랜잭션이 synchronized 블록보다 바깥이 되어 락 해제 후 커밋 → 상호배제가 깨진다.
 */
@Component
class SynchronizedDrawingStrategy(
    transactionManager: PlatformTransactionManager,
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
    override val mode = DrawMode.SYNCHRONIZED

    private val transactionTemplate = TransactionTemplate(transactionManager)

    // 설문별 JVM 로컬 모니터 객체. 인스턴스(JVM)마다 별개라는 점이 실험의 핵심.
    private val surveyLocks = ConcurrentHashMap<UUID, Any>()

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val lockObject = surveyLocks.computeIfAbsent(surveyId) { Any() }
        val waitStart = System.nanoTime()
        synchronized(lockObject) {
            drawingProcessMetrics.recordJvmLockWait(System.nanoTime() - waitStart)
            // 트랜잭션을 락 안에서 시작하고 커밋까지 마친 뒤 락을 놓는다
            return transactionTemplate.execute {
                draw(surveyId, participantId, selectedNumber, phoneNumber)
            }!!
        }
    }
}
