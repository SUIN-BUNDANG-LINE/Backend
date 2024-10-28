package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.NullableLoginUser
import com.sbl.sulmun2yong.survey.dto.request.SurveyResultRequest
import com.sbl.sulmun2yong.survey.dto.response.ParticipantsInfoListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyRawResultResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "SurveyManagement", description = "설문 관리 페이지 관련 API")
interface SurveyManagementApiDoc {
    @Operation(summary = "설문 결과 조회")
    @PostMapping("/result/{survey-id}")
    fun getSurveyResult(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestBody surveyResultRequest: SurveyResultRequest,
        @RequestParam participantId: UUID?,
        @RequestParam visitorId: String?,
    ): ResponseEntity<SurveyResultResponse>

    @Operation(summary = "설문 결과 raw 데이터 조회")
    @GetMapping("/raw-result/{survey-id}")
    fun getSurveyRawResult(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestParam visitorId: String?,
    ): ResponseEntity<SurveyRawResultResponse>

    @Operation(summary = "참가자 목록")
    @PostMapping("/participants/{survey-id}")
    fun getSurveyParticipants(
        @PathVariable("survey-id") surveyId: UUID,
        @NullableLoginUser makerId: UUID?,
        @RequestParam visitorId: String?,
    ): ResponseEntity<ParticipantsInfoListResponse>
}
