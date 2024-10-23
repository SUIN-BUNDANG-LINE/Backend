package com.sbl.sulmun2yong.user.util.dummy

import com.sbl.sulmun2yong.survey.domain.Participant
import java.util.UUID

object RandomParticipantGenerateUtil {
    // 한 설문 당 평균 50명 참여
    private val randomParticipantCountPicker =
        ProbabilityPicker(
            mapOf(
                0 to 0.2,
                10 to 0.1,
                30 to 0.3,
                50 to 0.2,
                100 to 0.1,
                200 to 0.1,
            ),
        )

    fun generateRandomParticipants(surveyId: UUID) =
        (1..randomParticipantCountPicker.pick()).map {
            generateRandomParticipant(surveyId)
        }

    private fun generateRandomParticipant(surveyId: UUID) =
        Participant.create(
            visitorId = UUID.randomUUID().toString(),
            surveyId = surveyId,
            userId = null,
        )
}
