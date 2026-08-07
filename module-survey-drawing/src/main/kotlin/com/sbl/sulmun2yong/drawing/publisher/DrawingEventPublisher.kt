package com.sbl.sulmun2yong.drawing.publisher

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

// drawing 도메인의 Kafka 발행 단일 진입점.
// 추첨 전략(DrawingStrategy) 구현 등 비즈니스 로직은 이 컴포넌트에만 의존한다.
@Component
class DrawingEventPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun publishCompleted(
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

        val event =
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
                kafkaTopic = KafkaTopics.DRAWING_COMPLETED,
                event = event,
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
