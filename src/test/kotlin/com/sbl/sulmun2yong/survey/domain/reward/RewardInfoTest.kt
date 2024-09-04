package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory
import com.sbl.sulmun2yong.survey.exception.InvalidRewardInfoException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RewardInfoTest {
    @Test
    fun `리워드 정보를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val rewards = SurveyFixtureFactory.REWARDS
        val targetParticipantCount = SurveyFixtureFactory.TARGET_PARTICIPANT_COUNT

        // when
        val immediateRewardInfo1 = ImmediateDrawRewardInfo(rewards, targetParticipantCount)
        val immediateRewardInfo2 = RewardInfo.of(rewards, targetParticipantCount)
        val freeRewardInfo1 = ByUserRewardInfo(listOf())
        val freeRewardInfo2 = RewardInfo.of(listOf(), null)

        // then
        assertEquals(rewards, immediateRewardInfo1.rewards)
        assertEquals(targetParticipantCount, immediateRewardInfo1.targetParticipantCount)
        assertEquals(true, immediateRewardInfo1.isImmediateDraw)
        assertEquals(rewards, immediateRewardInfo2.rewards)
        assertEquals(targetParticipantCount, immediateRewardInfo2.targetParticipantCount)
        assertEquals(true, immediateRewardInfo2.isImmediateDraw)
        assertEquals(emptyList(), freeRewardInfo1.rewards)
        assertEquals(null, freeRewardInfo1.targetParticipantCount)
        assertEquals(false, freeRewardInfo1.isImmediateDraw)
        assertEquals(emptyList(), freeRewardInfo2.rewards)
        assertEquals(null, freeRewardInfo2.targetParticipantCount)
        assertEquals(false, freeRewardInfo2.isImmediateDraw)
    }

    @Test
    fun `즉시 추첨은 리워드가 하나 이상 존재해야한다`() {
        assertThrows<InvalidRewardInfoException> {
            ImmediateDrawRewardInfo(listOf(), 0)
        }
    }

    @Test
    fun `즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야한다`() {
        assertThrows<InvalidRewardInfoException> {
            ImmediateDrawRewardInfo(SurveyFixtureFactory.REWARDS, 1)
        }
    }
}
