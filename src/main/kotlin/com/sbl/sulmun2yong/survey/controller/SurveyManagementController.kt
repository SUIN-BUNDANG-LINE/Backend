package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyManagementApiDoc
import com.sbl.sulmun2yong.survey.dto.SurveySaveRequest
import com.sbl.sulmun2yong.survey.service.SurveyManagementService
import org.springframework.http.ResponseEntity
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
    @PostMapping
    override fun createSurvey(
        @LoginUser id: UUID,
    ) = ResponseEntity.ok(surveyManagementService.createSurvey(makerId = id))

    // TODO: 수정할 수 있는 설문의 정보에 제한이 필요
    @PutMapping("/{surveyId}")
    override fun saveSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody surveySaveRequest: SurveySaveRequest,
    ) = ResponseEntity.ok(
        surveyManagementService.saveSurvey(
            surveySaveRequest = surveySaveRequest,
            makerId = id,
        ),
    )
}
