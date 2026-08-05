package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingProcessService
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Redis 분산락 (운영 기본) — 기존 운영 경로 [DrawingProcessService]
 * (`@RedissonLock` AOP + 트랜잭션 완료 후 언락)에 위임한다.
 */
@Component
class RedissonDrawingStrategy(
    private val drawingProcessService: DrawingProcessService,
) : DrawingStrategy {
    override val mode = DrawMode.REDISSON

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = drawingProcessService.processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
}
