package com.sbl.sulmun2yong.survey.listener

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.event.DrawingCompletedAutoCloseConsumedEvent
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

// drawing-completed 토픽의 drawing-auto-close groupId 메시지를 받아 티켓 소진 시 설문을 종료한다.
// Consumer 어댑터가 보유한 트랜잭션을 그대로 전파(REQUIRED) 받아 동일 트랜잭션 내에서 PESSIMISTIC 락을 사용한다.
@Component
class SurveyAutoCloseOnDrawingExhaustedEventListener(
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SurveyAutoCloseOnDrawingExhaustedEventListener::class.java)
    }

    @EventListener
    fun handle(event: DrawingCompletedAutoCloseConsumedEvent) {
        if (event.remainingTickets > 0) return

        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalseWithLock(UUID.fromString(event.surveyId))
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, surveyId: {}", survey.id)
            return
        }

        surveyRepository.save(survey.finish())
        log.info("티켓 소진으로 종료, surveyId: {}", survey.id)
    }
}
