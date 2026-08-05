package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.service.DrawingProcessCore
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM 로컬 직렬화 — 설문별 모니터에 synchronized 로 진입해 락 안에서 트랜잭션을 시작·커밋한다.
 * 모니터가 JVM 마다 별개라 cross-JVM 경합은 막지 못한다 — 그 한계를 실측하기 위한 경로 (비교 실험 전용).
 */
@Component
class SynchronizedDrawingStrategy(
    private val core: DrawingProcessCore,
    private val drawingProcessMetrics: DrawingProcessMetrics,
) : DrawingStrategy {
    // 설문별 JVM 로컬 모니터 객체. 인스턴스(JVM)마다 별개라는 점이 실험의 핵심.
    private val surveyLocks = ConcurrentHashMap<UUID, Any>()

    override val mode = DrawMode.SYNCHRONIZED

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
            return core.processDefault(surveyId, participantId, selectedNumber, phoneNumber)
        }
    }
}
