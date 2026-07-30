package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidRewardException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RewardTest {
    @Test
    fun `리워드를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val name = "품목 명"
        val category = "리워드 카테고리"
        val count = 1

        // when
        val reward = Reward(name = name, category = category, count = count)
        val defaultReward = Reward.create()

        // then
        with(reward) {
            assertEquals(name, this.name)
            assertEquals(category, this.category)
            assertEquals(count, this.count)
        }
        with(defaultReward) {
            assertEquals(Reward.DEFAULT_REWARD_NAME, this.name)
            assertEquals(Reward.DEFAULT_REWARD_CATEGORY, this.category)
            assertEquals(Reward.DEFAULT_REWARD_COUNT, this.count)
        }
    }

    @Test
    fun `리워드의 개수는 1이상이다`() {
        assertThrows<InvalidRewardException> {
            Reward(
                name = "품목 명",
                category = "리워드 카테고리",
                count = 0,
            )
        }
        assertThrows<InvalidRewardException> {
            Reward(
                name = "품목 명",
                category = "리워드 카테고리",
                count = -1,
            )
        }
    }
}
