package com.sbl.sulmun2yong.ai.scheduler

import com.sbl.sulmun2yong.ai.adapter.HealthCheckAdapter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AIWarmUpScheduler(
    private val healthCheckAdapter: HealthCheckAdapter,
) {
    private val log = LoggerFactory.getLogger(AIWarmUpScheduler::class.java)

    @Scheduled(cron = "0 */5  * * * *")
    fun aiWarmUp() {
        healthCheckAdapter.healthCheck()
        log.info("AI 서버 Health Check 성공")
    }
}
