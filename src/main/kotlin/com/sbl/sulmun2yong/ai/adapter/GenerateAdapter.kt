package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithFileUrlRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.GenerateSurveyResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyAIProcessingFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.domain.Survey
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateAdapter(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun requestSurveyGenerationWithFileUrl(
        target: String,
        groupName: String,
        fileUrl: String?,
        userPrompt: String,
        originalSurvey: Survey,
    ): AIGeneratedSurvey {
        val generateSurveyResponseFromPython =
            requestWithFileUrl(
                GenerateWithFileUrlRequestToPython(
                    chatSessionId = null,
                    target = target,
                    groupName = groupName,
                    userPrompt = userPrompt,
                    fileUrl = fileUrl,
                ),
            )

        return generateSurveyResponseFromPython.toDomain(originalSurvey)
    }

    private fun requestWithFileUrl(
        generateWithFileUrlRequestToPython: GenerateWithFileUrlRequestToPython,
    ): GenerateSurveyResponseFromPython = requestGenerateSurvey(generateWithFileUrlRequestToPython)

    private fun requestGenerateSurvey(requestBody: GenerateRequestToPython): GenerateSurveyResponseFromPython =
        try {
            requestToPythonServerTemplate
                .postForEntity(
                    "/generate/survey",
                    requestBody,
                    GenerateSurveyResponseFromPython::class.java,
                ).body ?: throw SurveyAIProcessingFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
}
