package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.dto.response.SurveyGenerateResponse
import com.sbl.sulmun2yong.global.util.FileValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class GenerateService(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    private val fileValidator: FileValidator,
) {
    fun generateSurvey(
        job: String,
        groupName: String,
        fileUrl: String,
    ): SurveyGenerateResponse {
        fileValidator.validateFileUrlOf(fileUrl)

        val restTemplate = RestTemplate()
        val url = "$aiServerBaseUrl/generate/survey"
        val requestBody =
            mapOf(
                "job" to job,
                "group_name" to groupName,
                "file_url" to fileUrl,
            )

        val response: ResponseEntity<SurveyGenerateResponse> =
            restTemplate.postForEntity(
                url,
                requestBody,
                SurveyGenerateResponse::class.java,
            )

        val responseBody = response.body ?: throw RuntimeException("Failed to generate survey")
        return responseBody
    }
}
