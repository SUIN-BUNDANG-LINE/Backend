package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "SurveyResponse", description = "설문 응답 관련 API")
interface SurveyResponseApiDoc {
    @Operation(summary = "설문 응답")
    @PostMapping
    fun responseToSurvey(
        @PathVariable("survey-id") surveyId: UUID,
        @Valid @RequestBody surveyResponseRequest: SurveyResponseRequest,
    ): ResponseEntity<SurveyParticipantResponse>
}
