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
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM 로컬 직렬화 (비교 실험 전용) — 설문별 모니터에 synchronized 로 진입한다.
 *
 * 경쟁 제어 지점: **JVM 로컬 모니터**. 모니터가 인스턴스(JVM)마다 별개라
 * 같은 JVM 안에서는 완벽히 직렬화되지만 cross-JVM 경합은 그대로 통과한다 — 그 한계를 실측한다.
 *
 * 트랜잭션은 `TransactionTemplate` 으로 **락 안에서** 열고 닫는다.
 * (`@Transactional` 을 이 메서드에 붙이면 트랜잭션이 synchronized 블록보다 바깥이 되어
 *  락 해제 후 커밋 → 상호배제가 깨진다. 락 경계와 트랜잭션 경계의 순서가 이 전략의 핵심이다.)
 */
@Component
class SynchronizedDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val surveyRepository: SurveyRepository,
    private val encryptionUtils: EncryptionUtils,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    private val drawingEventPublisher: DrawingEventPublisher,
) : DrawingStrategy {
    companion object {
        private val log = LoggerFactory.getLogger(SynchronizedDrawingStrategy::class.java)
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

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
            // 트랜잭션을 락 안에서 시작하고 커밋까지 마친 뒤 락을 놓는다
            return transactionTemplate.execute {
                draw(surveyId, participantId, selectedNumber, phoneNumber)
            }!!
        }
    }

    private fun draw(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 추첨 가능 여부 조회
        val drawingBoard =
            drawingBoardRepository
                .findBySurveyId(surveyId)
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

        // 추첨 결과 저장
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
