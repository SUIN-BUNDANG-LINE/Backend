package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "SurveyInfo", description = "설문 정보 관련 API")
interface SurveyInfoApiDoc {
    @Operation(summary = "설문 목록 페이지네이션 조회")
    @GetMapping("/surveys/list")
    fun getSurveysWithPagination(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "RECENT") sortType: SurveySortType,
        @RequestParam(defaultValue = "false") isAsc: Boolean,
    ): ResponseEntity<SurveyListResponse>

    @Operation(summary = "설문 정보 조회")
    @GetMapping("/surveys/info/{survey-id}")
    fun getSurveyInfo(
        @PathVariable("survey-id") surveyId: UUID,
    ): ResponseEntity<SurveyInfoResponse>
}
