package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.repository.ChatRepository
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ChatAdapter(
    private val chatRepository: ChatRepository,
) {
    fun requestEditSurveyWithChat(
        chatSessionId: UUID,
        survey: Survey,
        userPrompt: String,
    ): Survey {
        val pythonServerSurveyFormat = chatRepository.requestEditSurvey(chatSessionId, survey, userPrompt)
        return pythonServerSurveyFormat.toNewSurvey()
    }

    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ): Section {
        val pythonServerSectionFormat = chatRepository.requestEditSection(chatSessionId, section, userPrompt)
        val sectionId = section.id
        val sectionIds = section.sectionIds
        return pythonServerSectionFormat.toDomain(sectionId, sectionIds)
    }

    fun requestEditQuestionWithChat(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ): Question {
        val pythonServerQuestionFormat = chatRepository.requestEditQuestion(chatSessionId, question, userPrompt)
        return pythonServerQuestionFormat.toDomain()
    }
}
