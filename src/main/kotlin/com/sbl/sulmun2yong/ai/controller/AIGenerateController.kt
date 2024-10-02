package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.GenerateAPIDoc
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
import com.sbl.sulmun2yong.ai.service.GenerateService
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai/generate")
class AIGenerateController(
    private val generateService: GenerateService,
) : GenerateAPIDoc {
    @PostMapping("/survey/file-url")
    override fun generateSurveyWithFileUrl(
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiSurveyGenerationResponse = generateService.generateSurveyWithFileUrl(surveyGenerationWithFileUrlRequest)
        setChatSessionIdCookie(response, aiSurveyGenerationResponse.chatSessionId)
        return ResponseEntity.ok(aiSurveyGenerationResponse.generatedSurvey)
    }

    @PostMapping("/survey/text-document")
    override fun generateSurveyWithTextDocument(
        @RequestBody surveyGenerationWithTextDocumentRequest: SurveyGenerationWithTextDocumentRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        val aiSurveyGenerationResponse = generateService.generateSurveyWithTextDocument(surveyGenerationWithTextDocumentRequest)
        setChatSessionIdCookie(response, aiSurveyGenerationResponse.chatSessionId)
        return ResponseEntity.ok(aiSurveyGenerationResponse.generatedSurvey)
    }

    private fun setChatSessionIdCookie(
        response: HttpServletResponse,
        chatSessionId: UUID,
    ) {
        val cookie =
            Cookie("chat-session-id", chatSessionId.toString()).apply {
                maxAge = 3600
                path = "/"
                isHttpOnly = true
            }

        response.addCookie(cookie)
    }
}
