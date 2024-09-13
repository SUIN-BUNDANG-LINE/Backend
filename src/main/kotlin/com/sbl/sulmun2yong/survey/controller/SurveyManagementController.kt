package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyManagementApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.service.SurveyManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/surveys/workbench")
class SurveyManagementController(
    private val surveyManagementService: SurveyManagementService,
) : SurveyManagementApiDoc {
    @PostMapping("/create")
    override fun createSurvey(
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyManagementService.createSurvey(makerId = id))

    @PutMapping("/{surveyId}")
    override fun saveSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody surveySaveRequest: SurveySaveRequest,
    ) = ResponseEntity.ok(
        surveyManagementService.saveSurvey(
            surveySaveRequest = surveySaveRequest,
            makerId = id,
            surveyId = surveyId,
        ),
    )

    @GetMapping("/{surveyId}")
    override fun getSurveyMakeInfo(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyManagementService.getSurveyMakeInfo(surveyId = surveyId, makerId = id))

    @PatchMapping("/start/{surveyId}")
    override fun startSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyManagementService.startSurvey(surveyId = surveyId, makerId = id))

    @PatchMapping("/edit/{surveyId}")
    override fun editSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyManagementService.editSurvey(surveyId = surveyId, makerId = id))
}
