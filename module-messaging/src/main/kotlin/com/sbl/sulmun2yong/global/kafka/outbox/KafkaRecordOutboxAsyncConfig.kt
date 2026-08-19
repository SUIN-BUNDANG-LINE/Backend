package com.sbl.sulmun2yong.global.kafka.outbox

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

// 아웃박스가 자기 실행기를 챙긴다 - KafkaRecordOutboxEventListener·KafkaRecordOutboxService 의 @Async 가 요구하는
// outboxAsyncExecutor 를 여기서 제공한다. 서비스 모듈에 맡기면 @EnableAsync 를 빠뜨린 서비스에서
// @Async 가 조용히 무효화되고, 상태 전환 DB 왕복이 kafka-producer-network-thread 를 막는다.
@Configuration
@EnableAsync
class KafkaRecordOutboxAsyncConfig {
    @Bean
    fun outboxAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("outbox-async-")
        executor.setTaskDecorator(MdcPropagatingDecorator())
        executor.initialize()
        return executor
    }

    // 다른 스레드로 넘어가도 correlationId 가 로그에 남게 MDC 를 실어 보낸다.
    private class MdcPropagatingDecorator : TaskDecorator {
        override fun decorate(runnable: Runnable): Runnable {
            val contextMap = MDC.getCopyOfContextMap()

            return Runnable {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap)
                    }
                    runnable.run()
                } finally {
                    MDC.clear()
                }
            }
        }
    }
}
