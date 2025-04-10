package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.dto.response.AIHealthCheckResponse
import com.sbl.sulmun2yong.ai.exception.AIHealthCheckFailException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Component
class HealthCheckAdapter(
    private val requestToAIServerTemplate: RestTemplate,
) {
    fun healthCheck(): AIHealthCheckResponse =
        try {
            val responseEntity =
                requestToAIServerTemplate
                    .getForEntity<AIHealthCheckResponse>(
                        "/healthcheck",
                    ).body ?: throw AIHealthCheckFailException()
            responseEntity
        } catch (exception: Exception) {
            throw exception
        }
}
