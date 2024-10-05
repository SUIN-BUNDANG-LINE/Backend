package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import java.util.UUID

class PythonServerQuestionFormat(
    val questionType: QuestionType,
    val title: String,
    val required: Boolean,
    val choices: List<String>?,
    val allowOther: Boolean,
) {
    fun toDomain() =
        when (questionType) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.required,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), allowOther),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.required,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), allowOther),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.required,
                )
        }

    companion object {
        private const val DEFAULT_DESCRIPTION = ""

        fun of(question: Question) =
            PythonServerQuestionFormat(
                questionType = QuestionType.SINGLE_CHOICE,
                title = question.title,
                required = question.isRequired,
                choices = question.choices?.standardChoices?.map { it.content },
                allowOther = question.choices?.isAllowOther ?: false,
            )
    }
}
