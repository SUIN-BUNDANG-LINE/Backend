package com.sbl.sulmun2yong.survey.domain.question

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChoiceTest {
    @Test
    fun `선택지를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val content = "content"

        // when
        val standardChoice = Choice.Standard(content)
        val otherChoice = Choice.Other
        val notNullChoice = Choice.from(content)
        val nullChoice = Choice.from(null)

        // then
        assertEquals(content, standardChoice.content)
        assertEquals(null, otherChoice.content)
        assertEquals(content, notNullChoice.content)
        assertEquals(true, notNullChoice is Choice.Standard)
        assertEquals(null, nullChoice.content)
        assertEquals(true, nullChoice is Choice.Other)
    }
}
