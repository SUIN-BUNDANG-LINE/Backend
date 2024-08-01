package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.createSectionIds
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSectionIdsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class SectionIdTest {
    @Test
    fun `SectionId를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val id = UUID.randomUUID()

        // when
        val sectionId1 = SectionId.Standard(id)
        val sectionId2 = SectionId.End
        val sectionId3 = SectionId.from(id)
        val sectionId4 = SectionId.from(null)

        // then
        assertEquals(id, sectionId1.value)
        assertEquals(null, sectionId2.value)
        assertEquals(id, sectionId3.value)
        assertEquals(true, sectionId3 is SectionId.Standard)
        assertEquals(null, sectionId4.value)
        assertEquals(true, sectionId4 is SectionId.End)
    }

    @Test
    fun `SectionIds를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val id1 = SectionId.Standard(UUID.randomUUID())
        val id2 = SectionId.Standard(UUID.randomUUID())
        val ids = listOf(id1, id2)

        // when
        val sectionIds1 = SectionIds(ids + SectionId.End)
        val sectionIds2 = SectionIds.from(ids)

        // then
        assertEquals(ids + SectionId.End, sectionIds1.sectionIds)
        assertEquals(ids + SectionId.End, sectionIds2.sectionIds)
    }

    @Test
    fun `SectionIds는 비어있으면 안된다`() {
        assertThrows<InvalidSectionIdsException> { SectionIds(listOf()) }
    }

    @Test
    fun `SectionIds의 첫 번째 요소는 Standard여야한다`() {
        assertThrows<InvalidSectionIdsException> { SectionIds(listOf(SectionId.End, SectionId.Standard(UUID.randomUUID()))) }
    }

    @Test
    fun `SectionIds의 마지막 요소는 End여야한다`() {
        assertThrows<InvalidSectionIdsException> {
            SectionIds(listOf(SectionId.Standard(UUID.randomUUID()), SectionId.Standard(UUID.randomUUID())))
        }
    }

    @Test
    fun `SectionIds의 요소는 중복되면 안된다`() {
        // given
        val id1 = SectionId.Standard(UUID.randomUUID())
        val id2 = SectionId.Standard(UUID.randomUUID())
        val duplicatedIds1 = listOf(id1, id2, id1, SectionId.End)
        val duplicatedIds2 = listOf(id1, id2, SectionId.End, SectionId.End)

        // when, then
        assertThrows<InvalidSectionIdsException> { SectionIds(duplicatedIds1) }
        assertThrows<InvalidSectionIdsException> { SectionIds(duplicatedIds2) }
    }

    @Test
    fun `SectionIds는 섹션 ID를 받으면 해당 섹션의 다음 섹션 ID를 찾는다`() {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        val sectionIds = createSectionIds(listOf(id1, id2, id3))

        // when, then
        assertEquals(SectionId.Standard(id2), sectionIds.findNextSectionId(SectionId.Standard(id1)))
        assertEquals(SectionId.End, sectionIds.findNextSectionId(SectionId.Standard(id3)))
    }

    @Test
    fun `SectionIds는 섹션 ID List를 받아서 모든 섹션 ID가 포함되었는지 확인할 수 있다`() {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        val ids = listOf(SectionId.Standard(id1), SectionId.Standard(id2), SectionId.Standard(id3), SectionId.End)
        val sectionIds = createSectionIds(listOf(id1, id2, id3))

        assertEquals(true, sectionIds.isContainsAll(ids))
        assertEquals(true, sectionIds.isContainsAll(listOf()))
        assertEquals(false, sectionIds.isContainsAll(listOf(SectionId.Standard(UUID.randomUUID()))))
    }
}
