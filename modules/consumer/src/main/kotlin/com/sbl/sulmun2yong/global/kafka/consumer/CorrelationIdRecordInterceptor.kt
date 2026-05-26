package com.sbl.sulmun2yong.global.kafka.consumer

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.stereotype.Component
import java.lang.Exception
import java.nio.charset.StandardCharsets

@Component
class CorrelationIdRecordInterceptor : RecordInterceptor<Any, Any> {
    override fun intercept(
        record: ConsumerRecord<Any, Any>,
        consumer: Consumer<Any, Any>,
    ): ConsumerRecord<Any, Any>? {
        val id =
            record.headers().lastHeader("X-Correlation-Id")?.let { header ->
                String(header.value(), StandardCharsets.UTF_8)
            }
        if (id != null) {
            MDC.put("correlationId", id)
        }
        return record
    }

    override fun success(
        record: ConsumerRecord<Any, Any>,
        consumer: Consumer<Any, Any>,
    ) {
        MDC.clear()
    }

    override fun failure(
        record: ConsumerRecord<Any, Any>,
        exception: Exception,
        consumer: Consumer<Any, Any>,
    ) {
        MDC.clear()
    }
}
