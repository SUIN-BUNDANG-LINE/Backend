package com.sbl.sulmun2yong.global.lock

import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingLockKeyResolver(
    private val participantAdapter: ParticipantAdapter,
) {
    fun getLockKey(
        participantId: UUID,
        selectedNumber: Int,
    ): String {
        val surveyId = participantAdapter.getByParticipantId(participantId).surveyId
        return "drawingLock:$surveyId:$selectedNumber"
    }
}
