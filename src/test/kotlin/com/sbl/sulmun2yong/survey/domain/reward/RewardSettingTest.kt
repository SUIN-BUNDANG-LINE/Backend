package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory
import com.sbl.sulmun2yong.survey.exception.InvalidRewardSettingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RewardSettingTest {
    @Test
    fun `리워드 정보를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val rewards = SurveyFixtureFactory.REWARDS
        val targetParticipantCount = SurveyFixtureFactory.TARGET_PARTICIPANT_COUNT

        // when
        val immediateRewardSetting1 = ImmediateDrawRewardSetting(rewards, targetParticipantCount)
        val immediateRewardSetting2 = RewardSetting.of(rewards, targetParticipantCount)
        val freeRewardSetting1 = ByUserRewardSetting(listOf())
        val freeRewardSetting2 = RewardSetting.of(listOf(), null)

        // then
        assertEquals(rewards, immediateRewardSetting1.rewards)
        assertEquals(targetParticipantCount, immediateRewardSetting1.targetParticipantCount)
        assertEquals(true, immediateRewardSetting1.isImmediateDraw)
        assertEquals(rewards, immediateRewardSetting2.rewards)
        assertEquals(targetParticipantCount, immediateRewardSetting2.targetParticipantCount)
        assertEquals(true, immediateRewardSetting2.isImmediateDraw)
        assertEquals(emptyList(), freeRewardSetting1.rewards)
        assertEquals(null, freeRewardSetting1.targetParticipantCount)
        assertEquals(false, freeRewardSetting1.isImmediateDraw)
        assertEquals(emptyList(), freeRewardSetting2.rewards)
        assertEquals(null, freeRewardSetting2.targetParticipantCount)
        assertEquals(false, freeRewardSetting2.isImmediateDraw)
    }

    @Test
    fun `즉시 추첨은 리워드가 하나 이상 존재해야한다`() {
        assertThrows<InvalidRewardSettingException> {
            ImmediateDrawRewardSetting(listOf(), 0)
        }
    }

    @Test
    fun `즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야한다`() {
        assertThrows<InvalidRewardSettingException> {
            ImmediateDrawRewardSetting(SurveyFixtureFactory.REWARDS, 1)
        }
    }
}
