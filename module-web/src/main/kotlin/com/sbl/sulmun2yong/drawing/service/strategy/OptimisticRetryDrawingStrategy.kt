package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.service.DrawingProcessCore
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 낙관적 락 + 재시도 — OPTIMISTIC_FORCE_INCREMENT 로 보드 전체를 낙관적 자원화하고,
 * 버전 충돌·데드락을 트랜잭션 단위로 재시도한다 (비교 실험 전용).
 * 시도마다 원인별(drawing_attempts_total)로 기록해 성공당 시도 횟수를 계측한다.
 */
@Component
class OptimisticRetryDrawingStrategy(
    private val core: DrawingProcessCore,
    private val drawingProcessMetrics: DrawingProcessMetrics,
) : DrawingStrategy {
    companion object {
        private const val MAX_ATTEMPTS = 5
    }

    override val mode = DrawMode.OPTIMISTIC_RETRY

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        var lastFailure: RuntimeException? = null
        repeat(MAX_ATTEMPTS) {
            try {
                val result = core.processWithOptimisticForceIncrement(surveyId, participantId, selectedNumber, phoneNumber)
                drawingProcessMetrics.recordAttemptSuccess()
                return result
            } catch (e: ObjectOptimisticLockingFailureException) {
                drawingProcessMetrics.recordAttemptVersionConflict()
                lastFailure = e
            } catch (e: CannotAcquireLockException) {
                drawingProcessMetrics.recordAttemptDeadlock()
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("낙관락 재시도 소진")
    }
}
