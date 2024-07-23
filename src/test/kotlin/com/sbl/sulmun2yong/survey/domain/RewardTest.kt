package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidRewardException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class RewardTest {
    @Test
    fun `리워드의 개수는 1이상이다`() {
        assertDoesNotThrow {
            Reward(
                id = UUID.randomUUID(),
                name = "품목 명",
                category = "리워드 카테고리",
                count = 1,
            )
        }
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
