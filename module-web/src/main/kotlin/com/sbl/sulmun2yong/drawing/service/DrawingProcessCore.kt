package com.sbl.sulmun2yong.drawing.service

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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 추첨 트랜잭션 본체 — 모든 [com.sbl.sulmun2yong.drawing.service.strategy.DrawingStrategy] 구현이 공유한다.
 *
 * 전략 간 차이는 진입 방식(락 위치·격리수준·보드 조회 락 모드)뿐이므로 세 개의 트랜잭션 진입점만 두고
 * 본체 로직(추첨 판정 → 중복 참여 검증 → 저장 → 소진 종료 → 이벤트 발행)은 하나로 유지한다.
 */
@Service
class DrawingProcessCore(
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val surveyRepository: SurveyRepository,
    private val encryptionUtils: EncryptionUtils,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    private val drawingEventPublisher: DrawingEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DrawingProcessCore::class.java)
    }

    /** 기본 진입점 — NO_LOCK·SYNCHRONIZED·REDISSON 전략이 사용 (락은 각 전략이 트랜잭션 밖에서 처리) */
    @Transactional
    fun processDefault(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = doProcess(findBoard(surveyId), surveyId, participantId, selectedNumber, phoneNumber)

    /** SERIALIZABLE 전략 진입점 — 이 트랜잭션만 격리수준을 올린다 (재기동·전역 설정 불필요) */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun processSerializable(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse = doProcess(findBoard(surveyId), surveyId, participantId, selectedNumber, phoneNumber)

    /** 낙관락 전략 진입점 — OPTIMISTIC_FORCE_INCREMENT 조회로 보드 전체를 낙관적 자원화 */
    @Transactional
    fun processWithOptimisticForceIncrement(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse =
        doProcess(
            drawingBoardRepository
                .findBySurveyIdWithOptimisticForceIncrement(surveyId)
                .orElseThrow { InvalidDrawingBoardException() },
            surveyId,
            participantId,
            selectedNumber,
            phoneNumber,
        )

    private fun findBoard(surveyId: UUID): DrawingBoard =
        drawingBoardRepository
            .findBySurveyId(surveyId)
            .orElseThrow { InvalidDrawingBoardException() }

    private fun doProcess(
        drawingBoard: DrawingBoard,
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
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
        drawingProcessMetrics.recordPersisted(
            isWinner = drawingResult is DrawingResult.Winner,
        )

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
    // 동일 트랜잭션이라 추첨 결과와 설문 종료가 원자적으로 커밋된다.
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
