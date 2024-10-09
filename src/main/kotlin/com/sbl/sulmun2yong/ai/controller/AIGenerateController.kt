package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.AIGenerateApiDoc
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
import com.sbl.sulmun2yong.ai.service.GenerateService
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import jakarta.servlet.http.Cookie
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
    @PostMapping("/survey/file-url/{survey-id}")
    override fun generateSurveyWithFileUrl(
        @PathVariable("survey-id") surveyId: UUID,
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiSurveyGenerationResponse = generateService.generateSurveyWithFileUrl(surveyGenerationWithFileUrlRequest, surveyId)
        setChatSessionIdCookie(response, aiSurveyGenerationResponse.chatSessionId)
        return ResponseEntity.ok(aiSurveyGenerationResponse.generatedSurvey)
    }

    @PostMapping("/survey/text-document/{survey-id}")
    override fun generateSurveyWithTextDocument(
        @PathVariable("survey-id") surveyId: UUID,
        @RequestBody surveyGenerationWithTextDocumentRequest: SurveyGenerationWithTextDocumentRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiSurveyGenerationResponse = generateService.generateSurveyWithTextDocument(surveyGenerationWithTextDocumentRequest, surveyId)
        setChatSessionIdCookie(response, aiSurveyGenerationResponse.chatSessionId)
        return ResponseEntity.ok(aiSurveyGenerationResponse.generatedSurvey)
    }

    private fun setChatSessionIdCookie(
        response: HttpServletResponse,
        chatSessionId: UUID,
    ) {
        val cookie =
            Cookie("chat-session-id", chatSessionId.toString()).apply {
                maxAge = 60 * 60 * 24
                path = "/"
                isHttpOnly = true
            }

        response.addCookie(cookie)
    }
}
