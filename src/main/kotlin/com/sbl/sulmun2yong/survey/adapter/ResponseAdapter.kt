package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResponseAdapter(
    private val responseRepository: ResponseRepository,
) {
    fun insertSurveyResponse(
        surveyResponse: SurveyResponse,
        participantId: UUID,
    ) {
        responseRepository.insert(surveyResponse.toDocuments(participantId))
    }

    private fun SurveyResponse.toDocuments(participantId: UUID): List<ResponseDocument> =
        this.flatMap { sectionResponse ->
            sectionResponse.flatMap { questionResponse ->
                questionResponse.map {
                    ResponseDocument(
                        id = UUID.randomUUID(),
                        participantId = participantId,
                        surveyId = this.surveyId,
                        questionId = questionResponse.questionId,
                        content = it.content,
                    )
                }
            }
        }

    fun getResponses(surveyId: UUID): SurveyResult {
        val responses = responseRepository.findBySurveyId(surveyId)
        return SurveyResult(responses = responses.map { it.toDomain() })
    }

    private fun ResponseDocument.toDomain() =
        SurveyResult.Response(
            questionId = this.questionId,
            participantId = this.participantId,
            content = this.content,
            createdAt = this.createdAt,
        )
}
