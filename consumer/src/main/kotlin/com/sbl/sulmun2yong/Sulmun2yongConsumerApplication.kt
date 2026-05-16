package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

// Consumer 진입점 — Kafka 워커 + management-only Tomcat.
// web 측 빈(컨트롤러, Spring Security/JWT, OAuth2, Outbox Producer relay 등)은 ComponentScan에서 제외한다.
// Tomcat은 띄우되 컨트롤러가 한 개도 없으므로 비즈니스 API는 응답하지 않고,
// Actuator/Prometheus 엔드포인트만 노출한다 (Prometheus pull 기반 메트릭 수집을 위함).
@SpringBootApplication
@ComponentScan(
    basePackages = ["com.sbl.sulmun2yong"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASPECTJ,
            pattern = [
                // 모든 컨트롤러
                "com.sbl.sulmun2yong..controller..*",
                // OAuth2 인프라
                "com.sbl.sulmun2yong.global.config.oauth2..*",
                // Argument Resolver (MVC 전용)
                "com.sbl.sulmun2yong.global.resolver..*",
                // JWT 인증 필터
                "com.sbl.sulmun2yong.global.jwt.JwtAuthenticationFilter",
                // Web 전용 Configuration
                "com.sbl.sulmun2yong.global.config.SecurityConfig",
                "com.sbl.sulmun2yong.global.config.SwaggerConfig",
                "com.sbl.sulmun2yong.global.config.SessionConfig",
                "com.sbl.sulmun2yong.global.config.WebMvcConfig",
                // Web 전용 에러 핸들러
                "com.sbl.sulmun2yong.global.error.GlobalExceptionHandler",
                "com.sbl.sulmun2yong.global.error.CustomAccessDeniedHandler",
                "com.sbl.sulmun2yong.global.error.CustomAuthenticationEntryPoint",
                // Outbox Producer 측 (web 인스턴스에서만 발행)
                "com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventListener",
                "com.sbl.sulmun2yong.global.kafka.outbox.relay..*",
                "com.sbl.sulmun2yong.global.kafka.outbox.metrics..*",
                // AI 서버 헬스체크 (web 측 워밍업)
                "com.sbl.sulmun2yong.ai.scheduler..*",
            ],
        ),
    ],
)
class Sulmun2yongConsumerApplication

fun main(args: Array<String>) {
    runApplication<Sulmun2yongConsumerApplication>(*args)
}
