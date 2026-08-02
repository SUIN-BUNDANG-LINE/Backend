package com.sbl.sulmun2yong

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Web 진입점 — REST API 서버.
// consumer 모듈에 위치한 빈(KafkaListener, 도메인 listener, SMS worker 등)은 web JAR 클래스패스에 없으므로
// 자동으로 격리된다 (Gradle 의존성: web → common, web ↛ consumer).
@OpenAPIDefinition(servers = [Server(url = "\${backend.base-url}", description = "설문이용 서버")])
@SpringBootApplication
class Sulmun2yongApplication

fun main(args: Array<String>) {
    runApplication<Sulmun2yongApplication>(*args)
}
