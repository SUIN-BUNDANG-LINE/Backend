package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Consumer 진입점 — Kafka 워커 + management-only Tomcat.
// web 모듈에 위치한 빈(컨트롤러, Spring Security/JWT, OAuth2, Outbox Producer relay 등)은
// consumer JAR 클래스패스에 없으므로 자동으로 격리된다 (Gradle 의존성: consumer → common, consumer ↛ web).
// Tomcat은 띄우되 컨트롤러가 한 개도 없으므로 비즈니스 API는 응답하지 않고,
// Actuator/Prometheus 엔드포인트만 노출한다 (Prometheus pull 기반 메트릭 수집을 위함).
@SpringBootApplication
class Sulmun2yongConsumerApplication

fun main(args: Array<String>) {
    runApplication<Sulmun2yongConsumerApplication>(*args)
}
