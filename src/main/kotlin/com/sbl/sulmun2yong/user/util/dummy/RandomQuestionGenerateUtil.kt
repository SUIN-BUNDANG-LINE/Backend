package com.sbl.sulmun2yong.user.util.dummy

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import java.util.UUID

object RandomQuestionGenerateUtil {
    private val randomQuestionTypePicker =
        ProbabilityPicker(
            mapOf(
                QuestionType.SINGLE_CHOICE to 0.6,
                QuestionType.MULTIPLE_CHOICE to 0.3,
                QuestionType.TEXT_RESPONSE to 0.1,
            ),
        )

    private val randomIsRequired = Probability(0.7)
    private val randomIsAllowOther = Probability(0.2)

    private val randomChoiceCountPicker =
        ProbabilityPicker(
            mapOf(
                2 to 0.2,
                3 to 0.15,
                4 to 0.15,
                5 to 0.15,
                6 to 0.15,
                7 to 0.1,
                8 to 0.1,
            ),
        )

    fun generateRandomQuestion(index: Int): Question {
        val id = UUID.randomUUID()
        val title = "${index + 1}번 질문"
        val description = "${index + 1}번 질문 설명"
        val isRequired = randomIsRequired.isWinning()
        val questionType = randomQuestionTypePicker.pick()

        return when (questionType) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                    choices = generateRandomChoices(),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                    choices = generateRandomChoices(),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                )
        }
    }

    private fun generateRandomChoices(): Choices {
        val choiceCount = randomChoiceCountPicker.pick()
        val choices = (0 until choiceCount).map { "선택지 ${it + 1}" }
        return Choices(
            standardChoices = choices.map { Choice.Standard(it) },
            isAllowOther = randomIsAllowOther.isWinning(),
        )
    }
}
