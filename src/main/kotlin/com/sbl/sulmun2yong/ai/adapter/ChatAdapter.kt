package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.repository.ChatRepository
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
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
        return pythonServerSurveyFormat.toDomain()
    }

    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ): Section {
        val pythonServerSectionFormat = chatRepository.requestEditSection(chatSessionId, section, userPrompt)
        val sectionIds = listOf(SectionId.Standard(UUID.randomUUID()))
        val sectionIdsManger = SectionIds.from(sectionIds)
        return pythonServerSectionFormat.toDomain(sectionIds[0], sectionIdsManger)
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
