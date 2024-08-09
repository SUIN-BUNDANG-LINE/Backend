package com.sbl.sulmun2yong.survey.domain

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID
import kotlin.test.assertEquals

class ParticipantTest {
    @Test
    fun `참가자를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val participantId = UUID.randomUUID()
        val surveyId = UUID.randomUUID()
        val visitorId = "abcdefg"
        val userId = UUID.randomUUID()

        Mockito.mockStatic(UUID::class.java).use { mockedUUID ->
            mockedUUID.`when`<UUID> { UUID.randomUUID() }.thenReturn(participantId)

            // when
            val participant = Participant.create(visitorId, surveyId, userId)

            // then
            with(participant) {
                assertEquals(participantId, this.id)
                assertEquals(surveyId, this.surveyId)
                assertEquals(visitorId, this.visitorId)
                assertEquals(userId, this.userId)
            }
        }
    }
}
