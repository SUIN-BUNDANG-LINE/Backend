package com.sbl.sulmun2yong.drawing.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingExpiredEvent
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardStatus
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

// co-funding-expired(⑥) 구독 - 무산된 모금의 결제 대기 보드 폐기 리스너. drawing_boards 는 추첨만 쓴다(단일 기록자).
// 무산 = 모금이 완전히 끝난 사건이라 보드를 지워 설문 수정 잠금을 해제한다.
// 한 명의 결제 실패에는 반응하지 않는 것과 대칭 - 실패는 기한 내 재결제 가능이라 보드 유지,
// 기한 만료 무산만이 종착이라 폐기한다. 재접수는 판정이 보드를 새로 세우는 것으로 재개된다.
// 재전달·경쟁 안전: PENDING_PAYMENT 가드 - 무산 뒤 이미 열린(ACTIVE) 보드는 건드리지 않는다.
// 시간 가드: 무산 시각(expiredAt) 이후 태어난 보드는 재접수의 새 보드다 - 낡은 ⑥ 재전달이 지우면 안 된다.
@Component
class CoFundingExpiredDrawingListener(
    private val objectMapper: ObjectMapper,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingExpiredDrawingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_EXPIRED],
        groupId = "drawing-cofunding-expired",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingExpiredEvent::class.java)
        val expiredAt = LocalDateTime.ofInstant(event.expiredAt, ZoneId.systemDefault())
        drawingBoardRepository
            .findBySurveyId(UUID.fromString(event.surveyId))
            .filter { it.status == DrawingBoardStatus.PENDING_PAYMENT }
            .filter { it.createdAt.isBefore(expiredAt) }
            .ifPresent {
                drawingBoardRepository.delete(it)
                log.info("모금 무산 - 결제 대기 보드 폐기(수정 잠금 해제): surveyId={}", event.surveyId)
            }
        ack.acknowledge()
    }
}
