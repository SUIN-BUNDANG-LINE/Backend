package com.sbl.sulmun2yong.survey.scheduler

import com.sbl.sulmun2yong.global.error.GlobalExceptionHandler
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Date

@Service
class SurveyFinishScheduler(
    private val surveyRepository: SurveyRepository,
) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @Scheduled(cron = "0 0 * * * *") // 매 시 정각에 실행
    fun closeExpiredSurveys() {
        val targetSurveys = surveyRepository.findFinishTargets(Date())
        val finishedTargetSurveys =
            targetSurveys.map {
                log.info("설문 종료 시도: ${it.id}")
                it.finish()
            }

        surveyRepository.saveAll(finishedTargetSurveys)

        log.info("${targetSurveys.size}개의 설문을 성공적으로 종료했습니다.")
    }
}
