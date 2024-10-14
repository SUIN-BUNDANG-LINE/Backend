package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyWorkbenchApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.service.SurveyWorkbenchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
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
class SurveyWorkbenchController(
    private val surveyWorkbenchService: SurveyWorkbenchService,
) : SurveyWorkbenchApiDoc {
    @PostMapping("/create")
    override fun createSurvey(
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyWorkbenchService.createSurvey(makerId = id))

    @PutMapping("/{surveyId}")
    override fun saveSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody surveySaveRequest: SurveySaveRequest,
    ) = ResponseEntity.ok(
        surveyWorkbenchService.saveSurvey(
            surveySaveRequest = surveySaveRequest,
            makerId = id,
            surveyId = surveyId,
        ),
    )

    @PatchMapping("/start/{surveyId}")
    override fun startSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyWorkbenchService.startSurvey(surveyId = surveyId, makerId = id))

    @PatchMapping("/edit/{surveyId}")
    override fun editSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyWorkbenchService.editSurvey(surveyId = surveyId, makerId = id))

    @PatchMapping("/finish/{surveyId}")
    override fun finishSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyWorkbenchService.finishSurvey(surveyId = surveyId, makerId = id))

    @DeleteMapping("/delete/{surveyId}")
    override fun deleteSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyWorkbenchService.deleteSurvey(surveyId = surveyId, makerId = id))
}
