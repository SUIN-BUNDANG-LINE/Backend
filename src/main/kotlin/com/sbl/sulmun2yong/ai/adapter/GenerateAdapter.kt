package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.dto.SurveyGeneratedByAI
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateAdapter(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
) {
    private val url = "$aiServerBaseUrl/generate/survey"

    fun postRequestWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
    ): SurveyGeneratedByAI {
        val requestBody =
            mapOf(
                "job" to job,
                "group_name" to groupName,
                "file_url" to fileUrl,
            )

        val responseBody =
            try {
                RestTemplate()
                    .postForEntity(
                        url,
                        requestBody,
                        SurveyGeneratedByAI::class.java,
                    ).body ?: throw SurveyGenerationByAIFailedException()
            } catch (e: HttpClientErrorException) {
                throw PythonServerExceptionMapper.mapException(e)
            }

        val survey = Survey.of(responseBody)
        return SurveyMakeInfoResponse.of(survey)
    }
}
