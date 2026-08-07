package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 낙관적 락 + 재시도 (비교 실험 전용) — 락을 잡지 않고 커밋 시점 버전 검사로 충돌을 판정한다.
 *
 * 경쟁 제어 지점: **엔티티 버전(@Version)**. 티켓 선택 상태가 자식 행(TicketEntity)에 있어
 * 보드 행이 dirty 되지 않으므로, `OPTIMISTIC_FORCE_INCREMENT` 조회로 "이 보드를 읽고 커밋하는
 * 모든 트랜잭션은 버전을 올린다"를 강제해 보드 전체를 하나의 낙관적 자원으로 만든다.
 *
 * 실패(버전 충돌·데드락)는 트랜잭션 단위로 재시도하며, 시도마다 원인별로 기록해
 * 성공당 시도 횟수(재시도 낭비)를 계측한다. 재시도 루프는 트랜잭션 **밖**이어야 하므로
 * `TransactionTemplate` 으로 시도마다 새 트랜잭션을 연다.
 */
@Component
class OptimisticRetryDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val surveyRepository: SurveyRepository,
    private val encryptionUtils: EncryptionUtils,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    private val drawingEventPublisher: DrawingEventPublisher,
) : DrawingStrategy {
    companion object {
        private const val MAX_ATTEMPTS = 5
        private val log = LoggerFactory.getLogger(OptimisticRetryDrawingStrategy::class.java)
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

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
                // 버전 UPDATE 자체가 같은 행을 두드리는 쓰기라 데드락도 발생한다 — 이것도 재시도 대상
                drawingProcessMetrics.recordAttemptDeadlock()
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("낙관락 재시도 소진")
    }

    private fun draw(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 추첨 가능 여부 조회 — 조회만으로 커밋 시 보드 version 을 강제 증가시킨다
        val drawingBoard =
            drawingBoardRepository
                .findBySurveyIdWithOptimisticForceIncrement(surveyId)
                .orElseThrow { InvalidDrawingBoardException() }
        val drawingResult = drawingBoard.getDrawingResult(selectedNumber)

        // 이미 추첨 참여했는지 검증
        val phoneNumberData = PhoneNumber.createWithNonNullable(phoneNumber)
        val existingHistory =
            drawingHistoryRepository
                .findBySurveyIdAndParticipantIdOrPhoneNumber(
                    surveyId,
                    participantId,
                    encryptionUtils.encrypt(phoneNumberData.value),
                ).orElse(null)
        if (existingHistory != null) {
            throw AlreadyParticipatedDrawingException()
        }

        // 추첨 결과 저장 — 커밋 시점에 버전 검사가 일어나며, 진 쪽이 충돌 예외로 롤백된다
        val changedDrawingBoard = drawingResult.changedDrawingBoard
        drawingBoardRepository.save(changedDrawingBoard)
        drawingHistoryRepository.save(
            DrawingHistory.create(
                participantId = participantId,
                phoneNumber = phoneNumberData,
                surveyId = surveyId,
                selectedTicketIndex = selectedNumber,
                ticket = changedDrawingBoard.tickets[selectedNumber],
            ),
        )
        drawingProcessMetrics.recordPersisted(isWinner = drawingResult is DrawingResult.Winner)

        closeSurveyIfTicketsExhausted(surveyId, changedDrawingBoard)

        drawingEventPublisher.publishCompleted(
            surveyId = surveyId,
            participantId = participantId,
            selectedNumber = selectedNumber,
            changedDrawingBoard = changedDrawingBoard,
        )

        return when (drawingResult) {
            is DrawingResult.Winner -> DrawingResultResponse.Winner(drawingResult.rewardName)
            is DrawingResult.NonWinner -> DrawingResultResponse.NonWinner()
        }
    }

    // 잔여 티켓이 0이 된 시점에 추첨 트랜잭션 안에서 설문을 즉시 종료한다.
    private fun closeSurveyIfTicketsExhausted(
        surveyId: UUID,
        changedDrawingBoard: DrawingBoard,
    ) {
        if (changedDrawingBoard.remainingTicketCount > 0) return

        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalseWithLock(surveyId)
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, surveyId: {}", survey.id)
            return
        }

        surveyRepository.save(survey.finish())
        log.info("티켓 소진으로 종료, surveyId: {}", survey.id)
    }
}
