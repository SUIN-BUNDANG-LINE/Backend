package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.controller.doc.SurveyInfoApiDoc
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveysResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyProgressInfoResponse
import com.sbl.sulmun2yong.survey.service.SurveyInfoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/surveys")
class SurveyInfoController(
    private val surveyInfoService: SurveyInfoService,
) : SurveyInfoApiDoc {
    @GetMapping("/list")
    override fun getSurveysWithPagination(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "RECENT") sortType: SurveySortType,
        @RequestParam(defaultValue = "false") isAsc: Boolean,
    ): ResponseEntity<SurveyListResponse> =
        ResponseEntity.ok(
            surveyInfoService.getSurveysWithPagination(size, page, sortType, isAsc),
        )

    @GetMapping("/info/{survey-id}")
    override fun getSurveyInfo(
        @PathVariable("survey-id") surveyId: UUID,
    ): ResponseEntity<SurveyInfoResponse> = ResponseEntity.ok(surveyInfoService.getSurveyInfo(surveyId))

    @GetMapping("/progress/{survey-id}")
    override fun getSurveyProgressInfo(
        @PathVariable("survey-id") surveyId: UUID,
    ): ResponseEntity<SurveyProgressInfoResponse> = ResponseEntity.ok(surveyInfoService.getSurveyProgressInfo(surveyId))

    @GetMapping("/my-page")
    override fun getMyPageSurveys(
        @LoginUser userId: UUID,
        @RequestParam status: SurveyStatus?,
        @RequestParam(defaultValue = "LAST_MODIFIED") sortType: MySurveySortType,
    ): ResponseEntity<MyPageSurveysResponse> = ResponseEntity.ok(surveyInfoService.getMyPageSurveys(userId, status, sortType))

    @GetMapping("/make-info/{surveyId}")
    override fun getSurveyMakeInfo(
        @PathVariable("surveyId") surveyId: UUID,
    ) = ResponseEntity.ok(surveyInfoService.getSurveyMakeInfo(surveyId = surveyId))
}
