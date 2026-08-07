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
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * DB 안 직렬화 (비교 실험 전용) — 이 요청의 트랜잭션만 SERIALIZABLE 격리수준으로 연다.
 *
 * 경쟁 제어 지점: **트랜잭션 격리수준**. 일반 SELECT 가 공유 락(S) 읽기가 되어,
 * 같은 보드를 읽은 두 트랜잭션이 배타 락(X) 승격을 다투다 데드락으로 한쪽이 롤백된다.
 * 그 비용(데드락·행 락 대기·롤백)을 계측하는 것이 이 경로의 목적이다.
 */
@Component
class SerializableDrawingStrategy(
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val surveyRepository: SurveyRepository,
    private val encryptionUtils: EncryptionUtils,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    private val drawingEventPublisher: DrawingEventPublisher,
) : DrawingStrategy {
    companion object {
        private val log = LoggerFactory.getLogger(SerializableDrawingStrategy::class.java)
    }

    override val mode = DrawMode.SERIALIZABLE

    @Transactional(isolation = Isolation.SERIALIZABLE)
    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 추첨 가능 여부 조회 — SERIALIZABLE 이라 이 SELECT 가 공유 락(S) 읽기가 된다
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

        // 추첨 결과 저장 — 여기서 S락 → X락 승격이 일어나며 경합 시 데드락이 발생한다
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
