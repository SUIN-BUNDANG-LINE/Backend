package com.sbl.sulmun2yong.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

// outboxAsyncExecutor 는 :messaging 의 KafkaRecordOutboxAsyncConfig 가 제공한다 - 아웃박스를 쓰는 서비스가
// 그 실행기를 각자 챙기지 않아도 되게 인프라 쪽으로 옮겼다.
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun smsJobExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("sms-job-")
        executor.setTaskDecorator(MdcTaskDecorator())
        executor.initialize()
        return executor
    }
}
