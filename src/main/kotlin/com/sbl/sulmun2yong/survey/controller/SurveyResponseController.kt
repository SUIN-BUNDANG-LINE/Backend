package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.IsAdmin
import com.sbl.sulmun2yong.survey.controller.doc.SurveyResponseApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import com.sbl.sulmun2yong.survey.service.SurveyResponseService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/surveys/response")
class SurveyResponseController(
    private val surveyResponseService: SurveyResponseService,
) : SurveyResponseApiDoc {
    @PostMapping("/{survey-id}")
    override fun responseToSurvey(
        @PathVariable("survey-id") surveyId: UUID,
        @Valid @RequestBody surveyResponseRequest: SurveyResponseRequest,
        @IsAdmin isAdmin: Boolean,
    ): ResponseEntity<SurveyParticipantResponse> =
        ResponseEntity.ok(
            surveyResponseService.responseToSurvey(surveyId, surveyResponseRequest, isAdmin),
        )
}
