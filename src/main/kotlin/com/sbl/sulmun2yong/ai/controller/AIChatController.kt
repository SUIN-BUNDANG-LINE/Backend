package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.AIChatApiDoc
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.service.ChatService
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/ai/chat")
class AIChatController(
    private val chatService: ChatService,
) : AIChatApiDoc {
    @PostMapping("/edit/survey-data")
    override fun editSurveyWithChat(
        editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
        request: HttpServletRequest,
    ): ResponseEntity<SurveyMakeInfoResponse> {
        chatService.editSurveyDataWithChat(editSurveyDataWithChatRequest)
    }
}
