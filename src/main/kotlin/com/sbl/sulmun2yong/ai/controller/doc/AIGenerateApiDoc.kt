package com.sbl.sulmun2yong.ai.controller.doc

import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "AI Generation", description = "AI 생성 기능 관련 API")
interface AIGenerateApiDoc {
    @Operation(summary = "파일을 통한 AI 설문 생성")
    @PostMapping("/survey/{survey-id}")
    fun generateSurveyWithFileUrl(
        @PathVariable("survey-id") surveyId: UUID,
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse>
}
