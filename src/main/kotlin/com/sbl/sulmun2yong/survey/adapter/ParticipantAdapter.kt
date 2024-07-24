package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.entity.ParticipantDocument
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import org.springframework.stereotype.Component

@Component
class ParticipantAdapter(val participantRepository: ParticipantRepository) {
    fun saveParticipant(participant: Participant) {
        participantRepository.save(ParticipantDocument.of(participant))
    }
}
