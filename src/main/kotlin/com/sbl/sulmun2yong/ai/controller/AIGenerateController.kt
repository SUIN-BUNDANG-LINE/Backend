package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.AIGenerateApiDoc
import com.sbl.sulmun2yong.ai.dto.request.DemoSurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.service.GenerateService
import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai/generate")
class AIGenerateController(
    private val generateService: GenerateService,
) : AIGenerateApiDoc {
    @PostMapping("/survey/{survey-id}")
    override fun generateSurveyWithFileUrl(
        @PathVariable("survey-id") surveyId: UUID,
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        @LoginUser makerId: UUID,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiSurveyGenerationResponse = generateService.generateSurveyWithFileUrl(surveyGenerationWithFileUrlRequest, surveyId, makerId)
        return ResponseEntity.ok(aiSurveyGenerationResponse.generatedSurvey)
    }

    @PostMapping("/demo/survey/{visitor-id}")
    override fun generateDemoSurveyWithFileUrl(
        @RequestBody demoSurveyGenerationWithFileUrlRequest: DemoSurveyGenerationWithFileUrlRequest,
        @PathVariable("visitor-id") visitorId: String,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiDemoSurveyGenerationResponse =
            generateService.generateDemoSurveyWithFileUrl(
                demoSurveyGenerationWithFileUrlRequest,
                visitorId,
            )
        return ResponseEntity.ok(aiDemoSurveyGenerationResponse.generatedSurvey)
    }
}
