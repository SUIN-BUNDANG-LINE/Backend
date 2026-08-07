package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.NullableLoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyManagementApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveyResultRequest
import com.sbl.sulmun2yong.survey.service.SurveyManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/surveys/management")
class SurveyManagementController(
    private val surveyManagementService: SurveyManagementService,
) : SurveyManagementApiDoc {
    @PostMapping("/result/{survey-id}")
    override fun getSurveyResult(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestBody surveyResultRequest: SurveyResultRequest,
        @RequestParam participantId: UUID?,
        @RequestParam visitorId: String?,
    ) = ResponseEntity.ok(surveyManagementService.getSurveyResult(surveyId, makerId, surveyResultRequest, participantId, visitorId))

    @GetMapping("/raw-result/{survey-id}")
    override fun getSurveyRawResult(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestParam visitorId: String?,
    ) = ResponseEntity.ok(surveyManagementService.getSurveyRawResult(surveyId, makerId, visitorId))

    @GetMapping("/participants/{survey-id}")
    override fun getSurveyParticipants(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestParam visitorId: String?,
    ) = ResponseEntity.ok(surveyManagementService.getSurveyParticipants(surveyId, makerId, visitorId))
}
