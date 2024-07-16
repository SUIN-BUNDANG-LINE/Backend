package com.sbl.sulmun2yong.user.controller.doc

import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "SurveyInfo", description = "설문 정보 관련 API")
interface OAuth2ApiDoc {
    @Operation(summary = "현재 로그인된 전체 사용자 정보 조회")
    @GetMapping("/session")
    fun getCurrentSession(): ResponseEntity<SurveyListResponse>
}
