package com.sbl.sulmun2yong.global.kafka.consumer

import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

// 사가 리스너 공용 DLT 회로 - 리스너 처리 실패가 재시도로도 해소되지 않으면
// 원본 레코드를 saga.DLT 로 재발행하고 오프셋을 넘긴다(poison pill 이 파티션을 막지 않게).
// global.kafka 를 스캔하는 세 실행단위(web·모금·결제)의 모든 @KafkaListener 에 Boot 자동설정이 적용한다.
// SMS 발송 실패의 DLT(애플리케이션 레벨 명시 발행, KafkaSmsDltDispatcher)와는 별개 회로다.
@Configuration
class KafkaDltConfig {
    @Bean
    fun kafkaDltErrorHandler(kafkaTemplate: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        // 토픽별 "<topic>.DLT" 대신 saga.DLT 하나로 모은다 - 원본 토픽은 kafka_dlt-original-topic
        // 헤더에 보존되고 적재 리스너도 헤더에서 꺼내므로, 토픽을 쪼개 얻는 정보가 없다.
        // 파티션은 -1(키 해시 분배) - 원본 파티션 번호를 그대로 쓰면 파티션 수가 다를 때 깨진다.
        val recoverer =
            DeadLetterPublishingRecoverer(kafkaTemplate) { _, _ ->
                TopicPartition(KafkaTopics.SAGA_DLT, -1)
            }
        // 1초 간격 2회 재시도(총 3회 시도) 후 DLT - 일시 오류(DB 순단 등)는 흡수하고, 반복 실패만 격리한다.
        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L))
    }
}
