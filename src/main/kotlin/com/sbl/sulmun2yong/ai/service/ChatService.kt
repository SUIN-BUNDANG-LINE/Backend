package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val surveyAdapter: SurveyAdapter,
    private val chatAdapter: ChatAdapter,
) {
    fun editSurveyDataWithChat(editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest) {
        val surveyId = editSurveyDataWithChatRequest.surveyId
        val modificationTargetId = editSurveyDataWithChatRequest.modificationTargetId
        val survey = surveyAdapter.getSurvey(surveyId)

        survey.findSurveyById(modificationTargetId)
        survey.findSectionById(modificationTargetId)
        survey.findQuestionById(modificationTargetId)

        chatAdapter.requestEditSurveyWithChat()
        chatAdapter.requestEditSectionWithChat()
        chatAdapter.requestEditQuestionWithChat()
    }
}
