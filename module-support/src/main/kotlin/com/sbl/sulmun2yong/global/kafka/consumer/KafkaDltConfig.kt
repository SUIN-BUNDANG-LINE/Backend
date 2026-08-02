package com.sbl.sulmun2yong.global.kafka.consumer

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

// 사가 리스너 공용 DLT 회로 - 리스너 처리 실패가 재시도로도 해소되지 않으면
// 원본 레코드를 "<원본 토픽>.DLT" 로 재발행하고 오프셋을 넘긴다(poison pill 이 파티션을 막지 않게).
// global.kafka 를 스캔하는 세 실행단위(web·모금·결제)의 모든 @KafkaListener 에 Boot 자동설정이 적용한다.
// SMS 발송 실패의 DLT(애플리케이션 레벨 명시 발행, KafkaSmsDltDispatcher)와는 별개 회로다.
@Configuration
class KafkaDltConfig {
    @Bean
    fun kafkaDltErrorHandler(kafkaTemplate: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        // 기본 destination resolver: 같은 파티션 번호의 "<topic>.DLT" - 사가 토픽과 .DLT 모두 3파티션.
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        // 1초 간격 2회 재시도(총 3회 시도) 후 DLT - 일시 오류(DB 순단 등)는 흡수하고, 반복 실패만 격리한다.
        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L))
    }
}
