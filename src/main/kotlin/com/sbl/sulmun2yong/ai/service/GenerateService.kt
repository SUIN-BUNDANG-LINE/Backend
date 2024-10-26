package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.AIDemoCountRedisAdapter
import com.sbl.sulmun2yong.ai.adapter.GenerateAdapter
import com.sbl.sulmun2yong.ai.dto.request.DemoSurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyGenerationResponse
import com.sbl.sulmun2yong.global.fingerprint.FingerprintApi
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GenerateService(
    private val fileUrlValidator: FileUrlValidator,
    private val generateAdapter: GenerateAdapter,
    private val surveyAdapter: SurveyAdapter,
    private val aiDemoCountRedisAdapter: AIDemoCountRedisAdapter,
    val fingerprintApi: FingerprintApi,
) {
    fun generateSurveyWithFileUrl(
        surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        surveyId: UUID,
    ): AISurveyGenerationResponse {
        val target = surveyGenerationWithFileUrlRequest.target
        val groupName = surveyGenerationWithFileUrlRequest.groupName
        val fileUrl = surveyGenerationWithFileUrlRequest.fileUrl
        val userPrompt = surveyGenerationWithFileUrlRequest.userPrompt

        validateFileUrl(fileUrl)

        val survey = surveyAdapter.getSurvey(surveyId)

        return AISurveyGenerationResponse.from(
            generateAdapter.requestSurveyGenerationWithFileUrl(target, groupName, fileUrl, userPrompt, survey),
        )
    }

    fun generateDemoSurveyWithFileUrl(
        demoSurveyGenerationWithFileUrlRequest: DemoSurveyGenerationWithFileUrlRequest,
        visitorId: String,
    ): AISurveyGenerationResponse {
        val target = demoSurveyGenerationWithFileUrlRequest.target
        val groupName = demoSurveyGenerationWithFileUrlRequest.groupName
        val fileUrl = demoSurveyGenerationWithFileUrlRequest.fileUrl
        val userPrompt = demoSurveyGenerationWithFileUrlRequest.userPrompt

        validateFileUrl(fileUrl)
        fingerprintApi.validateVisitorId(visitorId)
        aiDemoCountRedisAdapter.incrementOrCreate(visitorId)

        val survey = Survey.create(UUID.randomUUID())

        return AISurveyGenerationResponse.from(
            generateAdapter.requestSurveyGenerationWithFileUrl(target, groupName, fileUrl, userPrompt, survey),
        )
    }

    private fun validateFileUrl(fileUrl: String?) {
        if (fileUrl != null) {
            fileUrlValidator.validateFileUrlOf(fileUrl, mutableListOf(".txt", ".pdf", ".pptx", ".docx"))
        }
    }
}
