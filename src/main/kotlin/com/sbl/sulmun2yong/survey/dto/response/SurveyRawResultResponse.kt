package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SurveyRawResultResponse(
    val rawResults: List<List<String>>,
) {
    companion object {
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
                val dateFormat = SimpleDateFormat("yyyy. M. d a h:mm:ss", Locale.KOREAN)
                dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val formattedDate = dateFormat.format(participant.createdAt)
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
