package com.sbl.sulmun2yong.user.util

import com.sbl.sulmun2yong.survey.domain.Participant
import java.util.UUID

object RandomParticipantGenerateUtil {
    // 한 설문 당 평균 111명 참여
    private val randomParticipantCountPicker =
        ProbabilityPicker(
            mapOf(
                0 to 0.05,
                1 to 0.05,
                3 to 0.1,
                5 to 0.3,
                10 to 0.35,
                20 to 0.1,
                50 to 0.025,
                100 to 0.025,
            ),
        )

    fun generateRandomParticipants(surveyId: UUID) =
        (1..randomParticipantCountPicker.pick() * 10).map {
            generateRandomParticipant(surveyId)
        }

    private fun generateRandomParticipant(surveyId: UUID) =
        Participant.create(
            visitorId = UUID.randomUUID().toString(),
            surveyId = surveyId,
            userId = null,
        )
}
