package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.survey.dto.SurveyCreateRequest
import com.sbl.sulmun2yong.survey.dto.SurveyCreateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "SurveyManagement", description = "설문 관리 관련 API")
interface SurveyManagementApiDoc {
    // TODO: 현재는 설문 제작을 편하게 하려고 급하게 만든 API이므로 추후 수정이 필요
    @Operation(summary = "설문 생성 API")
    @PostMapping("/create")
    fun createSurvey(
        @RequestBody surveyCreateRequest: SurveyCreateRequest,
    ): ResponseEntity<SurveyCreateResponse>
}
