package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.entity.ParticipantDocument
import com.sbl.sulmun2yong.survey.exception.InvalidParticipantException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ParticipantAdapter(
    val participantRepository: ParticipantRepository,
) {
    fun insert(participant: Participant) {
        participantRepository.insert(ParticipantDocument.of(participant))
    }

    fun getByParticipantId(id: UUID): Participant =
        participantRepository
            .findById(id)
            .orElseThrow { InvalidParticipantException() }
            .toDomain()

    fun findBySurveyId(surveyId: UUID): Participant? =
        participantRepository
            .findBySurveyId(surveyId)
            .map { it.toDomain() }
            .orElse(null)
}
