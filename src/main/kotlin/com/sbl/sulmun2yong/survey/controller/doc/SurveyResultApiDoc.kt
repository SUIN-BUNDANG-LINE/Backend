package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "SurveyResult", description = "설문 결과 관련 API")
interface SurveyResultApiDoc {
    @Operation(summary = "설문 결과 조회")
    @GetMapping("/{survey-id}")
    fun getSurveyResult(
        @PathVariable("survey-id") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<SurveyResultResponse>
}
