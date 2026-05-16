package com.sbl.sulmun2yong.survey.listener

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedAutoCloseConsumedEvent
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

// survey-response-submitted 토픽의 auto-close groupId 메시지를 받아 목표 참여자 수 도달 시 설문을 종료한다.
@Component
class SurveyAutoCloseOnTargetReachedEventListener(
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SurveyAutoCloseOnTargetReachedEventListener::class.java)
    }

    @EventListener
    fun handle(event: SurveyResponseSubmittedAutoCloseConsumedEvent) {
        if (event.targetParticipantCount == null || event.currentParticipantCount < event.targetParticipantCount) return

        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalseWithLock(UUID.fromString(event.surveyId))
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, surveyId: {}", survey.id)
            return
        }

        surveyRepository.save(survey.finish())
        log.info(
            "targetParticipantCount에 도달하여 설문을 종료합니다, 목표 참여자 수: {}, surveyId: {}",
            event.targetParticipantCount,
            survey.id,
        )
    }
}
