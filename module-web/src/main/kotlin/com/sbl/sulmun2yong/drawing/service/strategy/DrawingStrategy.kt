package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import java.util.UUID

/** 추첨 경쟁 제어 방식. REDISSON 이 운영 기본이며 나머지는 비교 실험 경로다. */
enum class DrawMode {
    NO_LOCK,
    SERIALIZABLE,
    OPTIMISTIC_RETRY,
    SYNCHRONIZED,
    REDISSON,
}

/**
 * 추첨 직렬화 전략 — 다섯 구현이 같은 계약으로 "경쟁을 어디서 제어하느냐"만 달리한다.
 *
 * 공통 검증(참가자 해석·설문 종료 여부)은 [com.sbl.sulmun2yong.drawing.service.DrawingBoardService]가
 * 수행한 뒤 전략에 위임하고, 추첨 트랜잭션 본체는 전 구현이 DrawingProcessCore 를 공유한다.
 */
interface DrawingStrategy {
    val mode: DrawMode

    fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse
}
