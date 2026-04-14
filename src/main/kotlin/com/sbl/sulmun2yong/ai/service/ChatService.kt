package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.ChatAdapter
import com.sbl.sulmun2yong.ai.dto.request.EditSurveyDataWithChatRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyEditResponse
import com.sbl.sulmun2yong.ai.entity.AIEditLog
import com.sbl.sulmun2yong.ai.exception.AIEditLogNotFoundException
import com.sbl.sulmun2yong.ai.exception.InvalidModificationTargetId
import com.sbl.sulmun2yong.ai.repository.AIEditLogRepository
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChatService(
    private val surveyRepository: SurveyRepository,
    private val chatAdapter: ChatAdapter,
    private val aiEditLogRepository: AIEditLogRepository,
) {
    fun editSurveyDataWithChat(
        makerId: UUID,
        editSurveyDataWithChatRequest: EditSurveyDataWithChatRequest,
    ): AISurveyEditResponse {
        val (surveyId, modificationTargetId, userPrompt, isEditGeneratedResult) = editSurveyDataWithChatRequest

        val originalSurvey =
            surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }

        val targetSurvey =
            if (isEditGeneratedResult) {
                aiEditLogRepository
                    .findFirstBySurveyIdAndMakerIdOrderByCreatedAtDesc(surveyId, makerId)
                    .orElseThrow { AIEditLogNotFoundException() }
                    .editedSurvey
            } else {
                originalSurvey
            }

        val updatedSurvey =
            targetSurvey.editSurveyWithAI(
                modificationTargetId = modificationTargetId,
                chatSessionId = surveyId,
                userPrompt = userPrompt,
            )

        aiEditLogRepository.save(
            AIEditLog.create(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                makerId = makerId,
                userPrompt = userPrompt,
                originalSurvey = targetSurvey,
                editedSurvey = updatedSurvey,
            ),
        )

        return AISurveyEditResponse.compareSurveys(originalSurvey, updatedSurvey)
    }

    /** 설문을 AI를 통해 수정하는 메서드 */
    private fun Survey.editSurveyWithAI(
        modificationTargetId: UUID,
        chatSessionId: UUID,
        userPrompt: String,
    ): Survey {
        if (this.id == modificationTargetId) {
            val pythonFormattedSurvey = chatAdapter.requestEditSurveyWithChat(chatSessionId, this, userPrompt)
            return pythonFormattedSurvey.toUpdatedSurvey(this)
        }

        this.findSectionById(modificationTargetId)?.let {
            val pythonFormattedSection = chatAdapter.requestEditSectionWithChat(chatSessionId, it, userPrompt)
            return pythonFormattedSection.toUpdatedSurvey(modificationTargetId, this)
        }

        this.findQuestionById(modificationTargetId)?.let {
            val pythonFormattedSection = chatAdapter.requestEditQuestionWithChat(chatSessionId, it, userPrompt)
            return pythonFormattedSection.toUpdatedSurvey(modificationTargetId, this)
        }

        throw InvalidModificationTargetId()
    }
}
