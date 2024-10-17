package com.sbl.sulmun2yong.user.util

import com.sbl.sulmun2yong.survey.domain.Participant
import java.util.UUID

object RandomParticipantGenerateUtil {
    // 한 설문 당 평균 113명 참여
    private val randomParticipantCountPicker =
        ProbabilityPicker(
            mapOf(
                0 to 0.075,
                10 to 0.1,
                30 to 0.2,
                50 to 0.2,
                100 to 0.25,
                200 to 0.1,
                500 to 0.05,
                1000 to 0.025,
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
