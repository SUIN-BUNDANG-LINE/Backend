package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.survey.controller.doc.SurveyManagementApiDoc
import com.sbl.sulmun2yong.survey.dto.SurveyCreateRequest
import com.sbl.sulmun2yong.survey.dto.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.service.SurveyManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/surveys")
class SurveyManagementController(
    private val surveyManagementService: SurveyManagementService,
) : SurveyManagementApiDoc {
    // TODO: 현재는 설문 제작을 편하게 하려고 급하게 만든 API이므로 추후 수정이 필요
    @PostMapping("/create")
    override fun createSurvey(
        @RequestBody surveyCreateRequest: SurveyCreateRequest,
    ): ResponseEntity<SurveyCreateResponse> = ResponseEntity.ok(surveyManagementService.createSurvey(surveyCreateRequest))
}
