package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.SurveyResponse
import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResponseAdapter(private val responseRepository: ResponseRepository) {
    fun saveSurveyResponse(
        surveyResponse: SurveyResponse,
        participantId: UUID,
    ) {
        responseRepository.saveAll(surveyResponse.toDocuments(participantId))
    }

    private fun SurveyResponse.toDocuments(participantId: UUID): List<ResponseDocument> {
        return this.flatMap { sectionResponse ->
            sectionResponse.flatMap { questionResponse ->
                questionResponse.map {
                    ResponseDocument(
                        id = UUID.randomUUID(),
                        participantId = participantId,
                        questionId = questionResponse.questionId,
                        content = it.content,
                    )
                }
            }
        }
    }
}
