package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.result.QuestionResult
import com.sbl.sulmun2yong.survey.domain.result.ResultDetails
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

    fun getSurveyResult(
        surveyId: UUID,
        participantId: UUID?,
    ): SurveyResult {
        val responses =
            if (participantId != null) {
                responseRepository.findBySurveyIdAndParticipantId(surveyId, participantId)
            } else {
                responseRepository.findBySurveyId(surveyId)
            }
        // TODO: 추후 DB Level에서 처리하도록 변경 + 필터링을 동적쿼리로 하도록 변경
        val groupingResponses = responses.groupBy { it.questionId }.values
        return SurveyResult(questionResults = groupingResponses.map { it.toDomain() })
    }

    private fun List<ResponseDocument>.toDomain() =
        QuestionResult(
            questionId = first().questionId,
            resultDetails =
                this.groupBy { it.participantId }.map {
                    ResultDetails(
                        participantId = it.key,
                        contents = it.value.map { responseDocument -> responseDocument.content },
                    )
                },
            contents = this.map { it.content }.toSortedSet(),
        )
}
