package com.sbl.sulmun2yong

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(servers = [Server(url = "\${backend.base-url}", description = "설문이용 서버")])
@SpringBootApplication
class Sulmun2yongApplication

fun main(args: Array<String>) {
    runApplication<Sulmun2yongApplication>(*args)
}
