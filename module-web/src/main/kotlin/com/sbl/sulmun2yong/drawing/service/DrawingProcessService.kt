package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.global.lock.RedissonLock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 운영 추첨 경로 — Redis 분산락으로 설문 단위 상호배제를 확보한 뒤 코어 트랜잭션을 실행한다.
 * 언락은 RedissonLockAspect 가 트랜잭션 완료(afterCompletion) 후 수행한다.
 */
@Service
class DrawingProcessService(
    private val core: DrawingProcessCore,
) {
    @RedissonLock(key = "drawingLock:{surveyId}", leaseTime = 10, waitTime = 5)
    @Transactional
    fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = core.processDefault(surveyId, participantId, selectedNumber, phoneNumber)
}
