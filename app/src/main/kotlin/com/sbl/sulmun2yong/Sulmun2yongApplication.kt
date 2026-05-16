package com.sbl.sulmun2yong

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

// Web 진입점 — REST API 서버.
// consumer 측 빈(KafkaListener, 도메인 listener, SMS worker 등)을 ComponentScan에서 제외해
// web 인스턴스가 Kafka 파티션을 점유하거나 워커 잡을 처리하지 않도록 한다.
@OpenAPIDefinition(servers = [Server(url = "\${backend.base-url}", description = "설문이용 서버")])
@SpringBootApplication
@ComponentScan(
    basePackages = ["com.sbl.sulmun2yong"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASPECTJ,
            pattern = [
                // Kafka 어댑터 + 페이로드
                "com.sbl.sulmun2yong.consumer..*",
                // 도메인 listener (Kafka consume 후 ApplicationEvent 처리)
                "com.sbl.sulmun2yong..listener..*",
                // SMS 보상 워커
                "com.sbl.sulmun2yong.notification.worker..*",
                // Consumer 측 Kafka Ack 인프라
                "com.sbl.sulmun2yong.global.kafka.consumer..*",
                // 시간 기반 자동 마감 스케줄러 (워커 측)
                "com.sbl.sulmun2yong.survey.scheduler..*",
            ],
        ),
    ],
)
class Sulmun2yongApplication

fun main(args: Array<String>) {
    runApplication<Sulmun2yongApplication>(*args)
}
