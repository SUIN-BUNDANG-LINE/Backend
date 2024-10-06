package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.PythonFormattedQuestion
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.EditQuestionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSectionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSurveyRequestToPython
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
    ): PythonFormattedSurvey =
        chatRepository
            .requestEditSurvey(
                EditSurveyRequestToPython(
                    chatSessionId = chatSessionId,
                    survey = PythonFormattedSurvey.of(survey),
                    userPrompt = userPrompt,
                ),
            ).toDomain()

    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ) = chatRepository
        .requestEditSection(
            EditSectionRequestToPython(
                chatSessionId = chatSessionId,
                section = PythonFormattedSection.of(section),
                userPrompt = userPrompt,
            ),
        ).toDomain()

    fun requestEditQuestionWithChat(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ) = chatRepository
        .requestEditQuestion(
            EditQuestionRequestToPython(
                chatSessionId = chatSessionId,
                question = PythonFormattedQuestion.of(question),
                userPrompt = userPrompt,
            ),
        ).toDomain()
}
