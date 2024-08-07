package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.dto.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.SurveySaveResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "SurveyManagement", description = "설문 관리 관련 API")
interface SurveyManagementApiDoc {
    @Operation(summary = "설문 생성 API")
    @PostMapping("/create")
    fun createSurvey(
        @LoginUser id: UUID,
    ): ResponseEntity<SurveyCreateResponse>

    // TODO: 추후 수정이 필요
    @Operation(summary = "설문 저장 API")
    @PutMapping("/save/{surveyId}")
    fun saveSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody surveySaveRequest: SurveySaveRequest,
    ): ResponseEntity<SurveySaveResponse>
}