package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyEditResponse
import com.sbl.sulmun2yong.ai.exception.InvalidModificationTargetId
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
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
    ): AISurveyEditResponse {
        val surveyId = editSurveyDataWithChatRequest.surveyId
        val modificationTargetId = editSurveyDataWithChatRequest.modificationTargetId
        val userPrompt = editSurveyDataWithChatRequest.userPrompt

        val targetSurvey: Survey = surveyAdapter.getSurvey(surveyId)

        val surveyOfTargetSurvey = targetSurvey.findSurveyById(modificationTargetId)
        if (surveyOfTargetSurvey != null) {
            val pythonFormattedSurvey = chatAdapter.requestEditSurveyWithChat(chatSessionId, surveyOfTargetSurvey, userPrompt)
            val updatedSurvey = pythonFormattedSurvey.toUpdatedSurvey(targetSurvey)
            surveyAdapter.save(updatedSurvey)

            return AISurveyEditResponse.of(updatedSurvey, targetSurvey, updatedSurvey)
        }

        val sectionOfTargetSurvey = targetSurvey.findSectionById(modificationTargetId)
        if (sectionOfTargetSurvey != null) {
            val pythonFormattedSection = chatAdapter.requestEditSectionWithChat(chatSessionId, sectionOfTargetSurvey, userPrompt)
            val updatedSurvey = pythonFormattedSection.toUpdatedSurvey(modificationTargetId, targetSurvey)
            surveyAdapter.save(updatedSurvey)

            return AISurveyEditResponse.of(
                updatedSurvey,
                sectionOfTargetSurvey,
                pythonFormattedSection.toSection(sectionOfTargetSurvey.id, sectionOfTargetSurvey.sectionIds),
            )
        }

        val questionOfTargetSurvey = targetSurvey.findQuestionById(modificationTargetId)
        if (questionOfTargetSurvey != null) {
            val pythonFormattedQuestion = chatAdapter.requestEditQuestionWithChat(chatSessionId, questionOfTargetSurvey, userPrompt)
            val updatedSurvey = pythonFormattedQuestion.toUpdatedSurvey(modificationTargetId, targetSurvey)
            surveyAdapter.save(updatedSurvey)

            return AISurveyEditResponse.of(updatedSurvey, questionOfTargetSurvey, pythonFormattedQuestion.toQuestion(modificationTargetId))
        }

        throw InvalidModificationTargetId()
    }
}
