package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.exception.InvalidModificationTargetId
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
        val userPrompt = editSurveyDataWithChatRequest.userPrompt

        val targetSurvey = surveyAdapter.getSurvey(surveyId)

        val surveyOfTargetSurvey = targetSurvey.findSurveyById(modificationTargetId)
        if (surveyOfTargetSurvey != null) {
            return chatAdapter.requestEditSurveyWithChat(chatSessionId, surveyOfTargetSurvey, userPrompt)
        }

        val sectionOfTargetSurvey = targetSurvey.findSectionById(modificationTargetId)
        if (sectionOfTargetSurvey != null) {
            return chatAdapter.requestEditSectionWithChat(chatSessionId, sectionOfTargetSurvey, userPrompt)
        }

        val questionOfTargetSurvey = targetSurvey.findQuestionById(modificationTargetId)
        if (questionOfTargetSurvey != null) {
            return chatAdapter.requestEditQuestionWithChat(chatSessionId, questionOfTargetSurvey, userPrompt)
        }

        throw InvalidModificationTargetId()
    }
}
