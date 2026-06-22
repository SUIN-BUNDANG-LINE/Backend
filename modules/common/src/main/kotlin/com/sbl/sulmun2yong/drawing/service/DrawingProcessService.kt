package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.lock.RedissonLock
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class DrawingProcessService(
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val encryptionUtils: EncryptionUtils,
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val drawingProcessMetrics: DrawingProcessMetrics,
) {
    @RedissonLock(key = "drawingLock:{surveyId}", leaseTime = 10, waitTime = 5)
    @Transactional
    fun processDrawing(
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
        drawingProcessMetrics.recordPersisted(
            isWinner = drawingResult is DrawingResult.Winner,
        )

        publishDrawingCompletedEvent(
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

    // 실험용 — 분산락 미적용. race condition 재현 비교 측정 전용.
    // 운영 코드 아님. T8 측정 종료 후 제거 가능.
    @Transactional
    fun processDrawingWithoutLock(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val drawingBoard =
            drawingBoardRepository
                .findBySurveyId(surveyId)
                .orElseThrow { InvalidDrawingBoardException() }
        val drawingResult = drawingBoard.getDrawingResult(selectedNumber)

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

        publishDrawingCompletedEvent(
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

    private fun publishDrawingCompletedEvent(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        changedDrawingBoard: DrawingBoard,
    ) {
        val selectedTicket = changedDrawingBoard.tickets[selectedNumber]
        val (rewardName, rewardCategory) =
            when (selectedTicket) {
                is Ticket.Winning -> selectedTicket.rewardName to selectedTicket.rewardCategory
                is Ticket.NonWinning -> null to null
            }

        val drawingCompletedEvent =
            DrawingCompletedEvent(
                eventId = UUID.randomUUID().toString(),
                surveyId = surveyId.toString(),
                participantId = participantId.toString(),
                selectedNumber = selectedNumber,
                isWinner = selectedTicket is Ticket.Winning,
                rewardName = rewardName,
                rewardCategory = rewardCategory,
                remainingTickets = changedDrawingBoard.remainingTicketCount,
                timestamp = Instant.now(),
            )

        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "Drawing",
                aggregateId = surveyId.toString(),
                eventType = "DrawingCompleted",
                kafkaTopic = "drawing-completed",
                event = drawingCompletedEvent,
            )

        outboxEventRepository.save(outboxEvent)

        applicationEventPublisher.publishEvent(
            OutboxPublishEvent(
                outboxId = outboxEvent.id,
                topic = outboxEvent.kafkaTopic,
                key = outboxEvent.kafkaKey,
                payload = outboxEvent.kafkaPayload,
            ),
        )
    }
}
