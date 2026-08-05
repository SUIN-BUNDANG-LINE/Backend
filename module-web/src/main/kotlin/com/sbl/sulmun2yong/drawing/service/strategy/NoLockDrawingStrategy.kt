package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingProcessCore
import org.springframework.stereotype.Component
import java.util.UUID

/** 무보호 기준선 — 아무 경쟁 제어 없이 코어 트랜잭션만 실행한다 (비교 실험 전용). */
@Component
class NoLockDrawingStrategy(
    private val core: DrawingProcessCore,
) : DrawingStrategy {
    override val mode = DrawMode.NO_LOCK

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = core.processDefault(surveyId, participantId, selectedNumber, phoneNumber)
}
