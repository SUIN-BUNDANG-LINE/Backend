package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.AIChatApiDoc
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyEditResponse
import com.sbl.sulmun2yong.ai.service.ChatService
import com.sbl.sulmun2yong.global.annotation.LoginUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("api/v1/ai/chat")
class AIChatController(
    private val chatService: ChatService,
) : AIChatApiDoc {
    @PostMapping("/edit/survey-data")
    override fun editSurveyWithChat(
        editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
        request: HttpServletRequest,
        @LoginUser id: UUID,
    ): ResponseEntity<AISurveyEditResponse> {
        val cookies = request.cookies
        val chatSessionId =
            cookies?.firstOrNull { it.name == "chat-session-id" }?.value ?: throw RuntimeException("chat-session-id cookie not found")

        return ResponseEntity.ok(
            chatService.editSurveyDataWithChat(UUID.fromString(chatSessionId), makerId = id, editSurveyDataWithChatRequest),
        )
    }
}
