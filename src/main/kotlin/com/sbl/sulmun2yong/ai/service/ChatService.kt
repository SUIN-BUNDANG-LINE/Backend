package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatAdapter: ChatAdapter,
) {
    fun editSurveyDataWithChat(editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest) {
        distinguishModificationTargetType()
        chatAdapter.requestEditSurveyWithChat()
        chatAdapter.requestEditSectionWithChat()
        chatAdapter.requestEditQuestionWithChat()
    }

    private fun distinguishModificationTargetType() {
    }
}
