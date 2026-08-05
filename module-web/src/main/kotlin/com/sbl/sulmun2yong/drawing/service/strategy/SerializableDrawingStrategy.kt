package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingProcessCore
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * DB 안 직렬화 — 이 요청의 트랜잭션만 SERIALIZABLE 격리수준으로 실행한다 (비교 실험 전용).
 * S락 읽기 → X락 승격 충돌이 데드락으로 터지는 비용을 계측하기 위한 경로.
 */
@Component
class SerializableDrawingStrategy(
    private val core: DrawingProcessCore,
) : DrawingStrategy {
    override val mode = DrawMode.SERIALIZABLE

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = core.processSerializable(surveyId, participantId, selectedNumber, phoneNumber)
}
