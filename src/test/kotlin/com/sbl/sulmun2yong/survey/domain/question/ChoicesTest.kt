package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidChoiceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ChoicesTest {
    @Test
    fun `선택지 목록을 생성하면 정보가 올바르게 설정된다`() {
        val list = listOf("1", "2", "3")
        val choices = Choices(list)
        assertEquals(list, choices)
    }

    @Test
    fun `선택지 목록은 비어있을 수 없다`() {
        assertThrows<InvalidChoiceException> { Choices(listOf()) }
    }

    @Test
    fun `선택지 목록의 내용은 중복될 수 없다`() {
        assertThrows<InvalidChoiceException> { Choices(listOf("1", "2", "2")) }
        assertThrows<InvalidChoiceException> { Choices(listOf("3", "3")) }
    }
}
