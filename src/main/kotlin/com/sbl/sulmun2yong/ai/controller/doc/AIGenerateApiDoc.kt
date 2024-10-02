package com.sbl.sulmun2yong.ai.controller.doc

import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "AI", description = "AI 기능 관련 API")
interface AIGenerateApiDoc {
    @Operation(summary = "파일을 통한 AI 설문 생성")
    @PostMapping("/survey/file-url")
    fun generateSurveyWithFileUrl(
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse>

    @Operation(summary = "텍스트 입력을 통한 AI 설문 생성")
    @PostMapping("/survey/text-document")
    fun generateSurveyWithTextDocument(
        @RequestBody surveyGenerationWithTextDocumentRequest: SurveyGenerationWithTextDocumentRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse>
}
