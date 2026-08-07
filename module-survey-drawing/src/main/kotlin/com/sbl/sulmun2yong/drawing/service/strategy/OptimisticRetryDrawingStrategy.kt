package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

/**
 * 낙관적 락 + 재시도 (비교 실험 전용) — 경쟁 제어 지점은 **엔티티 버전(@Version)**이다.
 *
 * 티켓 선택 상태가 자식 행(TicketEntity)에 있어 보드 행이 dirty 되지 않으므로,
 * 보드 조회를 `OPTIMISTIC_FORCE_INCREMENT` 로 바꿔 "이 보드를 읽고 커밋하는 모든 트랜잭션은
 * 버전을 올린다"를 강제해 보드 전체를 하나의 낙관적 자원으로 만든다.
 *
 * 실패(버전 충돌·데드락)는 트랜잭션 단위로 재시도하며, 시도마다 원인별로 기록해 성공당 시도
 * 횟수(재시도 낭비)를 계측한다. 재시도 루프는 트랜잭션 **밖**이어야 하므로 `TransactionTemplate`
 * 으로 시도마다 새 트랜잭션을 연다.
 */
@Component
class OptimisticRetryDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    private val boardRepository: DrawingBoardRepository,
    drawingHistoryRepository: DrawingHistoryRepository,
    surveyRepository: SurveyRepository,
    encryptionUtils: EncryptionUtils,
    drawingProcessMetrics: DrawingProcessMetrics,
    drawingEventPublisher: DrawingEventPublisher,
) : AbstractDrawingStrategy(
        boardRepository,
        drawingHistoryRepository,
        surveyRepository,
        encryptionUtils,
        drawingProcessMetrics,
        drawingEventPublisher,
    ) {
    override val mode = DrawMode.OPTIMISTIC_RETRY

    companion object {
        private const val MAX_ATTEMPTS = 5
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

    /** 조회만으로 커밋 시 보드 version 을 강제 증가시킨다 — 이 전략의 경쟁 제어 장치. */
    override fun findBoard(surveyId: UUID): DrawingBoard =
        boardRepository
            .findBySurveyIdWithOptimisticForceIncrement(surveyId)
            .orElseThrow { InvalidDrawingBoardException() }

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        var lastFailure: RuntimeException? = null
        repeat(MAX_ATTEMPTS) {
            try {
                // 시도마다 독립 트랜잭션 — 충돌로 롤백되면 다음 시도가 새 트랜잭션을 연다
                val result =
                    transactionTemplate.execute {
                        draw(surveyId, participantId, selectedNumber, phoneNumber)
                    }!!
                drawingProcessMetrics.recordAttemptSuccess()
                return result
            } catch (e: ObjectOptimisticLockingFailureException) {
                drawingProcessMetrics.recordAttemptVersionConflict()
                lastFailure = e
            } catch (e: CannotAcquireLockException) {
                // 버전 UPDATE 자체가 같은 행을 두드리는 쓰기라 데드락도 난다 — 이것도 재시도 대상
                drawingProcessMetrics.recordAttemptDeadlock()
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("낙관락 재시도 소진")
    }
}
