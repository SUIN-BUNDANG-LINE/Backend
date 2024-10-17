package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.survey.controller.doc.FakeSurveyResponseApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.service.FakeSurveyResponseService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@ConditionalOnProperty(prefix = "fingerprint", name = ["mocking-server-url"], matchIfMissing = false)
@RestController
@RequestMapping("/api/v1/surveys/response/fake")
class FakeSurveyResponseController(
    private val fakeSurveyResponseService: FakeSurveyResponseService,
) : FakeSurveyResponseApiDoc {
    @PostMapping("/{survey-id}")
    override fun fakeResponseToSurvey(
        @PathVariable("survey-id") surveyId: UUID,
        @Valid @RequestBody surveyResponseRequest: SurveyResponseRequest,
    ) = ResponseEntity.ok(fakeSurveyResponseService.fakeResponseToSurvey(surveyId, surveyResponseRequest))
}
