package com.sbl.sulmun2yong.ai.dto

import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import java.util.UUID

class QuestionGeneratedByAI(
    private val questionType: QuestionType,
    private val title: String,
    private val isRequired: Boolean,
    private val choices: List<String>?,
    private val isAllowOther: Boolean,
) {
    fun toDomain() =
        when (questionType) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.isRequired,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.isRequired,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = UUID.randomUUID(),
                    title = this.title,
                    description = DEFAULT_DESCRIPTION,
                    isRequired = this.isRequired,
                )
        }

    companion object {
        private const val DEFAULT_DESCRIPTION = ""
    }
}
