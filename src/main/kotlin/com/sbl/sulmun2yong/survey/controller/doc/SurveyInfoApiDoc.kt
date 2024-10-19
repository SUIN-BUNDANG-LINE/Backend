package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveysResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyProgressInfoResponse
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
    @GetMapping("/list")
    fun getSurveysWithPagination(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "RECENT") sortType: SurveySortType,
        @RequestParam reward: Boolean?,
        @RequestParam resultOpen: Boolean?,
    ): ResponseEntity<SurveyListResponse>

    @Operation(summary = "설문 정보 조회")
    @GetMapping("/info/{survey-id}")
    fun getSurveyInfo(
        @PathVariable("survey-id") surveyId: UUID,
    ): ResponseEntity<SurveyInfoResponse>

    @Operation(summary = "설문 진행 정보 조회")
    @GetMapping("/progress/{survey-id}")
    fun getSurveyProgressInfo(
        @PathVariable("survey-id") surveyId: UUID,
    ): ResponseEntity<SurveyProgressInfoResponse>

    @Operation(summary = "마이페이지 설문 목록 조회")
    @GetMapping("/my-page")
    fun getMyPageSurveys(
        @LoginUser userId: UUID,
        @RequestParam status: SurveyStatus?,
        @RequestParam(defaultValue = "LAST_MODIFIED") sortType: MySurveySortType,
    ): ResponseEntity<MyPageSurveysResponse>

    @Operation(summary = "설문 제작 정보 API")
    @GetMapping("/{surveyId}")
    fun getSurveyMakeInfo(
        @PathVariable("surveyId") surveyId: UUID,
    ): ResponseEntity<SurveyMakeInfoResponse>
}
