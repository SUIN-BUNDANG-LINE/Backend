package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyParticipantApiDoc
import com.sbl.sulmun2yong.survey.service.SurveyParticipantService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/surveys/participants")
class SurveyParticipantController(
    private val surveyParticipantService: SurveyParticipantService,
) : SurveyParticipantApiDoc {
    @GetMapping("/{survey-id}")
    override fun getSurveyParticipants(
        @PathVariable("survey-id") surveyId: UUID,
        @LoginUser userId: UUID,
    ) = ResponseEntity.ok(surveyParticipantService.getSurveyParticipants(surveyId, userId))
}
