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
        makerId: UUID,
        editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
    ): AISurveyEditResponse {
        val (surveyId, modificationTargetId, userPrompt, isEditGeneratedResult) = editSurveyDataWithChatRequest

        val survey = surveyAdapter.getByIdAndMakerId(surveyId = surveyId, makerId = makerId)

        val updatedSurvey =
            survey.editSurveyWithAI(
                modificationTargetId = modificationTargetId,
                chatSessionId = chatSessionId,
                userPrompt = userPrompt,
                isEditGeneratedResult = isEditGeneratedResult,
            )

        // 오리지널 설문과, AI가 수정한 설문을 비교한 결과를 반환.
        return AISurveyEditResponse.compareSurveys(survey, updatedSurvey)
    }

    /** 설문을 AI를 통해 수정하는 메서드 */
    private fun Survey.editSurveyWithAI(
        modificationTargetId: UUID,
        chatSessionId: UUID,
        userPrompt: String,
        isEditGeneratedResult: Boolean,
    ): Survey {
        if (this.id == modificationTargetId) {
            val pythonFormattedSurvey = chatAdapter.requestEditSurveyWithChat(chatSessionId, this, userPrompt, isEditGeneratedResult)
            return pythonFormattedSurvey.toUpdatedSurvey(this)
        }

        this.findSectionById(modificationTargetId)?.let {
            val pythonFormattedSection = chatAdapter.requestEditSectionWithChat(chatSessionId, it, userPrompt, isEditGeneratedResult)
            return pythonFormattedSection.toUpdatedSurvey(modificationTargetId, this)
        }

        this.findQuestionById(modificationTargetId)?.let {
            val pythonFormattedSection = chatAdapter.requestEditQuestionWithChat(chatSessionId, it, userPrompt, isEditGeneratedResult)
            return pythonFormattedSection.toUpdatedSurvey(modificationTargetId, this)
        }

        throw InvalidModificationTargetId()
    }
}
