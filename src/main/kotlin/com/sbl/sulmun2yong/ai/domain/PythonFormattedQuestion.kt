package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import java.util.UUID

class PythonFormattedQuestion(
    val id: UUID? = null,
    val questionType: QuestionType,
    val title: String,
    val description: String,
    val isRequired: Boolean,
    val choices: List<String>?,
    val isAllowOther: Boolean,
) {
    fun toQuestion() =
        when (questionType) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = id ?: UUID.randomUUID(),
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = id ?: UUID.randomUUID(),
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = id ?: UUID.randomUUID(),
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                )
        }

    fun toUpdatedSurvey(
        questionId: UUID,
        survey: Survey,
    ): Survey {
        val updatedSections =
            survey.sections.map { section ->
                val updatedQuestions =
                    section.questions.map { question ->
                        if (question.id == questionId) {
                            this.toQuestion()
                        } else {
                            question
                        }
                    }

                section.copy(questions = updatedQuestions)
            }

        return survey.updateContent(
            title = survey.title,
            description = survey.description,
            thumbnail = survey.thumbnail,
            finishMessage = survey.finishMessage,
            rewardSetting = survey.rewardSetting,
            isVisible = survey.isVisible,
            isResultOpen = survey.isResultOpen,
            sections = updatedSections,
        )
    }

    companion object {
        fun from(question: Question) =
            PythonFormattedQuestion(
                id = question.id,
                questionType = question.questionType,
                title = question.title,
                description = question.description,
                isRequired = question.isRequired,
                choices = question.choices?.standardChoices?.map { it.content },
                isAllowOther = question.choices?.isAllowOther ?: false,
            )
    }
}
