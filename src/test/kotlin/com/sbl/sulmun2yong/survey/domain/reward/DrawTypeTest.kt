package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory
import com.sbl.sulmun2yong.survey.exception.InvalidDrawTypeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DrawTypeTest {
    @Test
    fun `추첨 방식을 생성하면 정보가 올바르게 설정된다`() {
        // given
        val rewards = SurveyFixtureFactory.REWARDS
        val targetParticipantCount = SurveyFixtureFactory.TARGET_PARTICIPANT_COUNT

        // when
        val immediateDrawType1 = DrawType.Immediate(rewards, targetParticipantCount)
        val immediateDrawType2 = DrawType.of(rewards, targetParticipantCount)
        val freeDrawType1 = DrawType.Free(listOf())
        val freeDrawType2 = DrawType.of(listOf(), null)

        // then
        assertEquals(rewards, immediateDrawType1.rewards)
        assertEquals(targetParticipantCount, immediateDrawType1.targetParticipantCount)
        assertEquals(rewards, immediateDrawType2.rewards)
        assertEquals(targetParticipantCount, immediateDrawType2.targetParticipantCount)
        assertEquals(emptyList(), freeDrawType1.rewards)
        assertEquals(null, freeDrawType1.targetParticipantCount)
        assertEquals(emptyList(), freeDrawType2.rewards)
        assertEquals(null, freeDrawType2.targetParticipantCount)
    }

    @Test
    fun `즉시 추첨은 리워드가 하나 이상 존재해야한다`() {
        assertThrows<InvalidDrawTypeException> {
            DrawType.Immediate(listOf(), 0)
        }
    }

    @Test
    fun `즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야한다`() {
        assertThrows<InvalidDrawTypeException> {
            DrawType.Immediate(SurveyFixtureFactory.REWARDS, 1)
        }
    }
}
