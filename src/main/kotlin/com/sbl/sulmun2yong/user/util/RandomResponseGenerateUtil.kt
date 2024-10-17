package com.sbl.sulmun2yong.user.util

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.ChoiceQuestion
import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import java.util.UUID

object RandomResponseGenerateUtil {
    private val isResponded = Probability(0.6)

    /** 각 참가자별로 설문의 응답을 랜덤하게 생성하여 반환하는 메서드. */
    fun generateSurveyResponseDocuments(
        survey: Survey,
        participants: List<Participant>,
    ): List<ResponseDocument> {
        val questions = survey.sections.flatMap { it.questions }
        return questions.flatMap { question ->
            participants.mapNotNull { participant ->
                if (question.isRequired || isResponded.isWinning()) {
                    ResponseDocument(
                        id = UUID.randomUUID(),
                        participantId = participant.id,
                        surveyId = survey.id,
                        questionId = question.id,
                        content =
                            if (question is ChoiceQuestion) {
                                question.choices
                                    .getChoiceSet()
                                    .random()
                                    .content ?: "잘 모르겠어요"
                            } else {
                                "${question.title}에 대한 ${participant.visitorId}의 주관식 답변"
                            },
                    )
                } else {
                    null
                }
            }
        }
    }
}
