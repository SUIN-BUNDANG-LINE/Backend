package com.sbl.sulmun2yong.user.util.dummy

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import java.util.UUID
import kotlin.math.min

object RandomResponseGenerateUtil {
    private val isResponded = Probability(0.6)

    private val randomResponseCountPicker =
        ProbabilityPicker(
            mapOf(
                1 to 0.3,
                2 to 0.4,
                3 to 0.2,
                4 to 0.075,
                5 to 0.025,
            ),
        )

    /** 각 참가자별로 설문의 응답을 랜덤하게 생성하여 반환하는 메서드. */
    fun generateSurveyResponseDocuments(
        survey: Survey,
        participants: List<Participant>,
    ): List<ResponseDocument> {
        val questions = survey.sections.flatMap { it.questions }
        return questions.flatMap { question ->
            participants.flatMap { participant ->
                if (question.isRequired || isResponded.isWinning()) {
                    val contents = getRandomContents(question, participant.visitorId)
                    contents.map { content ->
                        ResponseDocument(
                            id = UUID.randomUUID(),
                            participantId = participant.id,
                            surveyId = survey.id,
                            questionId = question.id,
                            content = content,
                        )
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun getRandomContents(
        question: Question,
        visitorId: String,
    ): List<String> {
        if (question is SingleChoiceQuestion) {
            return pickRandomChoiceContent(question.choices, 1)
        }
        if (question is MultipleChoiceQuestion) {
            return pickRandomChoiceContent(question.choices, min(randomResponseCountPicker.pick(), question.choices.getChoiceSet().size))
        }
        return listOf(getTextResponseContent(question.title, visitorId))
    }

    private fun getTextResponseContent(
        title: String,
        visitorId: String,
    ): String = "${title}에 대한 ${visitorId}의 주관식 답변"

    private fun pickRandomChoiceContent(
        choices: Choices,
        count: Int,
    ): List<String> =
        choices
            .getChoiceSet()
            .shuffled()
            .take(count)
            .map { it.content ?: "잘 모르겠어요" }
}
