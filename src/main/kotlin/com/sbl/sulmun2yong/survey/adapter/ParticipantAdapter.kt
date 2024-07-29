package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.entity.ParticipantDocument
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ParticipantAdapter(
    val participantRepository: ParticipantRepository,
) {
    fun saveParticipant(participant: Participant) {
        participantRepository.save(ParticipantDocument.of(participant))
    }

    fun getParticipantById(id: UUID): Participant =
        participantRepository
            .findById(id)
            .orElseThrow { IllegalArgumentException("추첨 안한 사용자임") }
            .toDomain()
}
