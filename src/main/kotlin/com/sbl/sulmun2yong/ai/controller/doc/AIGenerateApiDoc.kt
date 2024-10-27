package com.sbl.sulmun2yong.ai.controller.doc

import com.sbl.sulmun2yong.ai.dto.request.DemoSurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.global.annotation.LoginUser
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
    @Operation(summary = "AI 설문 생성")
    @PostMapping("/survey/{survey-id}")
    fun generateSurveyWithFileUrl(
        @PathVariable("survey-id") surveyId: UUID,
        @RequestBody surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        @LoginUser makerId: UUID,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse>

    @Operation(summary = "데모 AI 설문 생성")
    @PostMapping("/demo/survey/{visitor-id}")
    fun generateDemoSurveyWithFileUrl(
        @RequestBody demoSurveyGenerationWithFileUrlRequest: DemoSurveyGenerationWithFileUrlRequest,
        @PathVariable("visitor-id") visitorId: String,
        response: HttpServletResponse,
    ): ResponseEntity<SurveyMakeInfoResponse>
}
