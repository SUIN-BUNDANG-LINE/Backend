package com.sbl.sulmun2yong.ai.controller.doc

import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerateRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "AI", description = "AI 기능 관련 API")
interface GenerateAPIDoc {
    @Operation(summary = "AI 설문 생성")
    @PostMapping("/survey")
    fun generateSurvey(
        @RequestBody request: SurveyGenerateRequest,
    ): ResponseEntity<SurveyMakeInfoResponse>
}
