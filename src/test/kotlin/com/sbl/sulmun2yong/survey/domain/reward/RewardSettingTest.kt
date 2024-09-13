package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.exception.InvalidFinishedAtException
import com.sbl.sulmun2yong.survey.exception.InvalidRewardSettingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Calendar
import kotlin.test.assertEquals

class RewardSettingTest {
    @Test
    fun `리워드 설정을 생성하면 정보가 올바르게 설정된다`() {
        // given
        val rewards = SurveyFixtureFactory.REWARDS
        val targetParticipantCount = SurveyFixtureFactory.TARGET_PARTICIPANT_COUNT
        val finishedAt = SurveyFixtureFactory.FINISHED_AT

        // when
        val immediateRewardSetting1 = ImmediateDrawSetting(rewards, targetParticipantCount, FinishedAt(finishedAt))
        val immediateRewardSetting2 = RewardSetting.of(RewardSettingType.IMMEDIATE_DRAW, rewards, targetParticipantCount, finishedAt)
        val selfManagementSetting1 = SelfManagementSetting(rewards, FinishedAt(finishedAt))
        val selfManagementSetting2 = RewardSetting.of(RewardSettingType.SELF_MANAGEMENT, rewards, null, finishedAt)
        val noRewardSetting1 = NoRewardSetting
        val noRewardSetting2 = RewardSetting.of(RewardSettingType.NO_REWARD, listOf(), null, null)

        // then
        // 즉시 추첨
        with(immediateRewardSetting1) {
            assertEquals(RewardSettingType.IMMEDIATE_DRAW, this.type)
            assertEquals(rewards, this.rewards)
            assertEquals(targetParticipantCount, this.targetParticipantCount)
            assertEquals(FinishedAt(finishedAt), this.finishedAt)
            assertEquals(true, this.isImmediateDraw)
        }
        with(immediateRewardSetting2) {
            assertEquals(RewardSettingType.IMMEDIATE_DRAW, this.type)
            assertEquals(rewards, this.rewards)
            assertEquals(targetParticipantCount, this.targetParticipantCount)
            assertEquals(FinishedAt(finishedAt), this.finishedAt)
            assertEquals(true, this.isImmediateDraw)
        }
        // 직접 추첨
        with(selfManagementSetting1) {
            assertEquals(RewardSettingType.SELF_MANAGEMENT, this.type)
            assertEquals(rewards, this.rewards)
            assertEquals(null, this.targetParticipantCount)
            assertEquals(FinishedAt(finishedAt), this.finishedAt)
            assertEquals(false, this.isImmediateDraw)
        }
        with(selfManagementSetting2) {
            assertEquals(RewardSettingType.SELF_MANAGEMENT, this.type)
            assertEquals(rewards, this.rewards)
            assertEquals(null, this.targetParticipantCount)
            assertEquals(FinishedAt(finishedAt), this.finishedAt)
            assertEquals(false, this.isImmediateDraw)
        }
        // 리워드 미 지급
        with(noRewardSetting1) {
            assertEquals(RewardSettingType.NO_REWARD, this.type)
            assertEquals(emptyList(), this.rewards)
            assertEquals(null, this.targetParticipantCount)
            assertEquals(null, this.finishedAt)
            assertEquals(false, this.isImmediateDraw)
        }
        with(noRewardSetting2) {
            assertEquals(RewardSettingType.NO_REWARD, this.type)
            assertEquals(emptyList(), this.rewards)
            assertEquals(null, this.targetParticipantCount)
            assertEquals(null, this.finishedAt)
            assertEquals(false, this.isImmediateDraw)
        }
    }

    @Test
    fun `리워드 설정를 잘못 생성하면 예외가 발생한다`() {
        // 리워드 미지급
        with(RewardSettingType.NO_REWARD) {
            // 리워드가 존재하는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, SurveyFixtureFactory.REWARDS, null, null)
            }
            // targetParticipant가 존재하는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, emptyList(), 100, null)
            }
            // finishedAt이 존재하는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, emptyList(), null, SurveyFixtureFactory.FINISHED_AT)
            }
        }
        // 직접 지급
        with(RewardSettingType.SELF_MANAGEMENT) {
            // 리워드가 없는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, emptyList(), 100, SurveyFixtureFactory.FINISHED_AT)
            }
            // targetParticipant가 존재하는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, SurveyFixtureFactory.REWARDS, 100, SurveyFixtureFactory.FINISHED_AT)
            }
            // finishedAt이 없는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, SurveyFixtureFactory.REWARDS, null, null)
            }
        }
        // 즉시 추첨
        with(RewardSettingType.IMMEDIATE_DRAW) {
            // 리워드가 없는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, emptyList(), 100, SurveyFixtureFactory.FINISHED_AT)
            }
            // targetParticipant가 존재하지 않는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, SurveyFixtureFactory.REWARDS, null, SurveyFixtureFactory.FINISHED_AT)
            }
            // finishedAt이 없는 경우 예외 발생
            assertThrows<InvalidRewardSettingException> {
                RewardSetting.of(this, SurveyFixtureFactory.REWARDS, 100, null)
            }
        }
    }

    @Test
    fun `즉시 추첨은 리워드가 하나 이상 존재해야한다`() {
        assertThrows<InvalidRewardSettingException> {
            ImmediateDrawSetting(listOf(), 10, FinishedAt(SurveyFixtureFactory.FINISHED_AT))
        }
    }

    @Test
    fun `즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야한다`() {
        assertThrows<InvalidRewardSettingException> {
            ImmediateDrawSetting(SurveyFixtureFactory.REWARDS, 1, FinishedAt(SurveyFixtureFactory.FINISHED_AT))
        }
    }

    @Test
    fun `직접 지급은 리워드가 하나 이상 존재해야한다`() {
        assertThrows<InvalidRewardSettingException> {
            SelfManagementSetting(listOf(), FinishedAt(SurveyFixtureFactory.FINISHED_AT))
        }
    }

    @Test
    fun `설문 종료일을 생성하면 정보가 올바르게 설정된다`() {
        // given
        val date = SurveyFixtureFactory.FINISHED_AT

        // when
        val finishedAt = FinishedAt(date)

        // then
        assertEquals(date, finishedAt.value)
    }

    @Test
    fun `설문 종료일은 분 단위 이하가 0이여야 한다`() {
        // given
        val calendar = Calendar.getInstance()
        calendar.time = DateUtil.getCurrentDate()
        calendar.set(Calendar.MINUTE, 1)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date1 = calendar.time
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 1)
        val date2 = calendar.time
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 1)
        val date3 = calendar.time

        // when, then
        assertThrows<InvalidFinishedAtException> { FinishedAt(date1) }
        assertThrows<InvalidFinishedAtException> { FinishedAt(date2) }
        assertThrows<InvalidFinishedAtException> { FinishedAt(date3) }
    }
}
