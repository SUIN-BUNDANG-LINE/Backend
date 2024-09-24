package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.survey.dto.response.ParticipantInfoListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

@Tag(name = "SurveyParticipant", description = "설문 참가자 관련 API")
interface SurveyParticipantApiDoc {
    @Operation(summary = "참가자 목록")
    @PostMapping
    fun getSurveyParticipants(
        @PathVariable("survey-id") surveyId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<ParticipantInfoListResponse>
}
