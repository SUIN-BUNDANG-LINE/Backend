package com.sbl.sulmun2yong.ai.controller.doc

import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyEditResponse
import com.sbl.sulmun2yong.global.annotation.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "AI Chat", description = "AI 채팅 기능 관련 API")
interface AIChatApiDoc {
    @Operation(summary = "채팅을 통한 AI 설문 수정")
    @PostMapping("/chat/edit")
    fun editSurveyWithChat(
        @RequestBody editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
        request: HttpServletRequest,
        @LoginUser id: UUID,
    ): ResponseEntity<AISurveyEditResponse>
}
