package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.dto.request.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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

    @Operation(summary = "설문 저장 API")
    @PutMapping("/save/{surveyId}")
    fun saveSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody surveySaveRequest: SurveySaveRequest,
    ): ResponseEntity<Unit>

    @Operation(summary = "설문 제작 정보 API")
    @GetMapping("/{surveyId}")
    fun getSurveyMakeInfo(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<SurveyMakeInfoResponse>

    @Operation(summary = "설문 시작 API")
    @PatchMapping("/start/{surveyId}")
    fun startSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<Unit>

    @Operation(summary = "설문 수정 API")
    @PatchMapping("/edit/{surveyId}")
    fun editSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<Unit>

    @Operation(summary = "설문 종료 API")
    @PatchMapping("/finish/{surveyId}")
    fun finishSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<Unit>

    @Operation(summary = "설문 삭제 API")
    @DeleteMapping("/delete/{surveyId}")
    fun deleteSurvey(
        @PathVariable("surveyId") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<Unit>
}
