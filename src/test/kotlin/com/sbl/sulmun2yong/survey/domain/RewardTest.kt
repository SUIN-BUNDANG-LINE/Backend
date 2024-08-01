package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidRewardException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RewardTest {
    @Test
    fun `리워드를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val name = "품목 명"
        val category = "리워드 카테고리"
        val count = 1

        // when
        val reward = Reward(id = id, name = name, category = category, count = count)

        // then
        assertEquals(id, reward.id)
        assertEquals(name, reward.name)
        assertEquals(category, reward.category)
        assertEquals(count, reward.count)
    }

    @Test
    fun `리워드의 개수는 1이상이다`() {
        assertThrows<InvalidRewardException> {
            Reward(
                id = UUID.randomUUID(),
                name = "품목 명",
                category = "리워드 카테고리",
                count = 0,
            )
        }
        assertThrows<InvalidRewardException> {
            Reward(
                id = UUID.randomUUID(),
                name = "품목 명",
                category = "리워드 카테고리",
                count = -1,
            )
        }
    }
}
