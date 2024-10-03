package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChatService(
    private val surveyAdapter: SurveyAdapter,
    private val chatAdapter: ChatAdapter,
) {
    fun editSurveyDataWithChat(
        chatSessionId: UUID,
        editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
    ): SurveyMakeInfoResponse {
        val surveyId = editSurveyDataWithChatRequest.surveyId
        val modificationTargetId = editSurveyDataWithChatRequest.modificationTargetId
        val targetSurvey = surveyAdapter.getSurvey(surveyId)

        val survey = targetSurvey.findSurveyById(modificationTargetId)!!
        val section = targetSurvey.findSectionById(modificationTargetId)!!
        val question = targetSurvey.findQuestionById(modificationTargetId)!!

        chatAdapter.requestEditSurveyWithChat(chatSessionId, survey, editSurveyDataWithChatRequest.userPrompt)
        chatAdapter.requestEditSectionWithChat(chatSessionId, section, editSurveyDataWithChatRequest.userPrompt)
        return chatAdapter.requestEditQuestionWithChat(chatSessionId, question, editSurveyDataWithChatRequest.userPrompt)
    }
}
