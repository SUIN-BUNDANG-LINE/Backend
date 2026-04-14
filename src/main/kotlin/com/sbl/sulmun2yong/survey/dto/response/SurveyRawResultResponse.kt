package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import com.sbl.sulmun2yong.survey.entity.Participant
import com.sbl.sulmun2yong.survey.entity.Survey
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SurveyRawResultResponse(
    val rawResults: List<List<String>>,
) {
    companion object {
        private val dateFormatter =
            DateTimeFormatter
                .ofPattern("yyyy. M. d a h:mm:ss", Locale.KOREAN)
                .withZone(ZoneId.of("Asia/Seoul"))

        fun of(
            survey: Survey,
            surveyResult: SurveyResult,
            participants: List<Participant>,
        ): SurveyRawResultResponse {
            val rawResults = mutableListOf<MutableList<String>>()

            val titles = mutableListOf("참가 일시")
            survey.sections.forEach { section ->
                section.questions.forEach { question ->
                    titles.add(question.title)
                }
            }
            rawResults.add(titles)

            participants.forEach { participant ->
                val formattedDate = participant.createdAt.format(dateFormatter)
                val response = mutableListOf(formattedDate)
                survey.sections
                    .flatMap { section ->
                        section.questions.mapNotNull { question ->
                            surveyResult.findQuestionResult(question.id)?.let { questionResult ->
                                questionResult.resultDetails
                                    .find { it.participantId == participant.id }
                                    ?.contents
                                    ?.joinToString(",") ?: ""
                            }
                        }
                    }.forEach { response.add(it) }
                rawResults.add(response)
            }

            return SurveyRawResultResponse(rawResults)
        }
    }
}
