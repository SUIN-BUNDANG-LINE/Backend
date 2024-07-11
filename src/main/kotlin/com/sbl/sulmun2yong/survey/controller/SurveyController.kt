package com.sbl.sulmun2yong.survey.controller

import com.sbl.sulmun2yong.survey.controller.doc.SurveyInfoApiDoc
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.service.SurveyInfoService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class SurveyController(private val surveyInfoService: SurveyInfoService) : SurveyInfoApiDoc {
    @GetMapping("/surveys/list")
    override fun getSurveysWithPagination(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "RECENT") sortType: SurveySortType,
        @RequestParam(defaultValue = "false") isAsc: Boolean,
    ): ResponseEntity<SurveyListResponse> {
        return ResponseEntity.ok(
            surveyInfoService.getSurveysWithPagination(size, page, sortType, isAsc),
        )
    }
}
