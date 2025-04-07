package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.exception.AIHealthCheckFailException
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Component
class HealthCheckAdapter(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun healthCheck() =
        try {
            requestToPythonServerTemplate
                .getForEntity<Unit>(
                    "/healthcheck",
                ).body ?: throw AIHealthCheckFailException()
        } catch (e: HttpClientErrorException) {
            throw AIHealthCheckFailException()
        }
}
