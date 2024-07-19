package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals

class SurveyTest {
    private val id = UUID.randomUUID()
    private val title = "설문 제목"
    private val description = "설문 설명"
    private val thumbnail = "설문 썸네일"
    private val finishMessage = "설문이 종료되었습니다."
    private val targetParticipants = 100
    private val finishedAt = Date.from(Instant.now())
    private val rewards =
        listOf(
            Reward(UUID.randomUUID(), "아메리카노", "커피", 3),
            Reward(UUID.randomUUID(), "카페라떼", "커피", 2),
            Reward(UUID.randomUUID(), "햄버거", "음식", 4),
        )

    @Test
    fun `설문을 생성하면 설문의 정보들이 설정된다`() {
        // given
        val publishedAt = null
        val status = SurveyStatus.NOT_STARTED
        val sections = listOf(Section.create())

        // when
        val survey =
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = publishedAt,
                status = status,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = sections,
            )

        // then
        with(survey) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(thumbnail, this.thumbnail)
            assertEquals(finishedAt, this.finishedAt)
            assertEquals(publishedAt, this.publishedAt)
            assertEquals(status, this.status)
            assertEquals(finishMessage, this.finishMessage)
            assertEquals(targetParticipants, this.targetParticipants)
            assertEquals(rewards, this.rewards)
            assertEquals(sections, this.sections)
        }
    }

    @Test
    fun `설문을 생성할 때 섹션이 1개 이상 없으면 예외가 발생한다`() {
        assertThrows<InvalidSurveyException> {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = listOf(),
            )
        }
    }

    @Test
    fun `설문을 생성할 때 섹션들의 ID가 중복되면 예외가 발생한다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()

        val section1 = mock<Section>()
        `when`(section1.id).thenReturn(sectionId1)
        `when`(section1.findNextSectionId(any())).thenReturn(sectionId2)
        `when`(section1.getDestinationSectionIdSet()).thenReturn(setOf(sectionId2, null))
        val section2 = mock<Section>()
        `when`(section2.id).thenReturn(sectionId2)
        `when`(section2.findNextSectionId(any())).thenReturn(sectionId3)
        `when`(section2.getDestinationSectionIdSet()).thenReturn(setOf(null))
        val section3 = mock<Section>()
        `when`(section3.id).thenReturn(sectionId3)
        `when`(section3.findNextSectionId(any())).thenReturn(null)
        `when`(section3.getDestinationSectionIdSet()).thenReturn(setOf(null))

        assertDoesNotThrow {
            Survey(
                id = UUID.randomUUID(),
                title = "a",
                description = "elementum",
                thumbnail = "option",
                publishedAt = Date(finishedAt.time - 24 * 60 * 60 * 10000),
                finishedAt = finishedAt,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = "b",
                targetParticipants = 200,
                rewards = rewards,
                sections = listOf(section1, section2, section3),
            )
        }

        assertThrows<InvalidSurveyException> {
            Survey(
                id = UUID.randomUUID(),
                title = "a",
                description = "elementum",
                thumbnail = "option",
                publishedAt = Date(finishedAt.time - 24 * 60 * 60 * 10000),
                finishedAt = finishedAt,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = "b",
                targetParticipants = 200,
                rewards = rewards,
                sections = listOf(section1, section2, section1),
            )
        }
    }

    @Test
    fun `설문의 시작일이 마감일 이후면 예외가 발생한다`() {
        // given
        val publishedAt = Date(finishedAt.time + 24 * 60 * 60 * 1000)
        val status = SurveyStatus.IN_PROGRESS
        val sections = listOf(Section.create())

        // when, then
        assertThrows<InvalidSurveyException> {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = publishedAt,
                status = status,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = sections,
            )
        }
    }

    @Test
    fun `설문의 시작일은 설문이 시작 전일 때만 null이다`() {
        assertThrows<InvalidSurveyException> {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = null,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = listOf(Section.create()),
            )
        }

        assertThrows<InvalidSurveyException> {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = null,
                status = SurveyStatus.CLOSED,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = listOf(Section.create()),
            )
        }
    }

    @Test
    fun `설문은 응답의 섹션 순서가 유효한지 검증할 수 있다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()
        val sectionId4 = UUID.randomUUID()

        val section1 = mock<Section>()
        `when`(section1.id).thenReturn(sectionId1)
        `when`(section1.findNextSectionId(any())).thenReturn(sectionId2)
        `when`(section1.getDestinationSectionIdSet()).thenReturn(setOf(sectionId2, sectionId3))
        val section2 = mock<Section>()
        `when`(section2.id).thenReturn(sectionId2)
        `when`(section2.findNextSectionId(any())).thenReturn(sectionId3)
        `when`(section2.getDestinationSectionIdSet()).thenReturn(setOf(null, sectionId3))
        val section3 = mock<Section>()
        `when`(section3.id).thenReturn(sectionId3)
        `when`(section3.findNextSectionId(any())).thenReturn(null)
        `when`(section3.getDestinationSectionIdSet()).thenReturn(setOf(null))
        val section4 = mock<Section>()
        `when`(section4.id).thenReturn(sectionId4)
        `when`(section4.findNextSectionId(any())).thenReturn(null)
        `when`(section4.getDestinationSectionIdSet()).thenReturn(setOf(null))

        val id = UUID.randomUUID()
        val survey =
            Survey(
                id = id,
                title = "a",
                description = "elementum",
                thumbnail = "option",
                publishedAt = Date(finishedAt.time - 24 * 60 * 60 * 10000),
                finishedAt = finishedAt,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = "b",
                targetParticipants = 200,
                rewards = rewards,
                sections = listOf(section1, section2, section3),
            )

        val surveyResponse1 =
            SurveyResponse(
                id,
                listOf(
                    SectionResponse(sectionId1, listOf()),
                    SectionResponse(sectionId2, listOf()),
                    SectionResponse(sectionId3, listOf()),
                ),
            )

        val surveyResponse2 =
            SurveyResponse(
                id,
                listOf(
                    SectionResponse(sectionId1, listOf()),
                    SectionResponse(sectionId2, listOf()),
                    SectionResponse(sectionId4, listOf()),
                ),
            )

        // when, then
        assertDoesNotThrow { survey.validateResponse(surveyResponse1) }
        assertThrows<InvalidSurveyResponseException> { survey.validateResponse(surveyResponse2) }
    }

    @Test
    fun `설문은 설문의 리워드 개수를 계산할 수 있다`() {
        // given
        val publishedAt = null
        val status = SurveyStatus.NOT_STARTED
        val sections = listOf(Section.create())

        // when
        val survey =
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = publishedAt,
                status = status,
                finishMessage = finishMessage,
                targetParticipants = targetParticipants,
                rewards = rewards,
                sections = sections,
            )

        val count = survey.getRewardCount()

        assertEquals(9, count)
    }

    @Test
    fun `설문의 리워드 개수는 목표 참여자 수 이하이다`() {
        assertDoesNotThrow {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                finishMessage = finishMessage,
                targetParticipants = 9,
                rewards = rewards,
                sections = listOf(Section.create()),
            )
        }

        assertThrows<InvalidSurveyException> {
            Survey(
                id = id,
                title = title,
                description = description,
                thumbnail = thumbnail,
                finishedAt = finishedAt,
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                finishMessage = finishMessage,
                targetParticipants = 8,
                rewards = rewards,
                sections = listOf(Section.create()),
            )
        }
    }

    @Test
    fun `설문에 속한 각 섹션의 RouteDetails의 SectionId는 설문에 속한 섹션의 ID여야 한다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()
        val sectionId4 = UUID.randomUUID()

        val section1 = mock<Section>()
        `when`(section1.id).thenReturn(sectionId1)
        `when`(section1.findNextSectionId(any())).thenReturn(sectionId2)
        `when`(section1.getDestinationSectionIdSet()).thenReturn(setOf(sectionId1, sectionId2, sectionId3))
        val section2 = mock<Section>()
        `when`(section2.id).thenReturn(sectionId2)
        `when`(section2.findNextSectionId(any())).thenReturn(sectionId3)
        `when`(section2.getDestinationSectionIdSet()).thenReturn(setOf(sectionId1))
        val section3 = mock<Section>()
        `when`(section3.id).thenReturn(sectionId3)
        `when`(section3.findNextSectionId(any())).thenReturn(null)
        `when`(section3.getDestinationSectionIdSet()).thenReturn(setOf(sectionId1, sectionId3))
        val section4 = mock<Section>()
        `when`(section4.id).thenReturn(sectionId4)
        `when`(section4.findNextSectionId(any())).thenReturn(null)
        `when`(section4.getDestinationSectionIdSet()).thenReturn(setOf(sectionId2, sectionId3))

        // when, then
        assertDoesNotThrow {
            Survey(
                id = UUID.randomUUID(),
                title = "a",
                description = "elementum",
                thumbnail = "option",
                publishedAt = Date(finishedAt.time - 24 * 60 * 60 * 10000),
                finishedAt = finishedAt,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = "b",
                targetParticipants = 200,
                rewards = rewards,
                sections = listOf(section1, section2, section3),
            )
        }

        assertThrows<InvalidSurveyException> {
            Survey(
                id = UUID.randomUUID(),
                title = "a",
                description = "elementum",
                thumbnail = "option",
                publishedAt = Date(finishedAt.time - 24 * 60 * 60 * 10000),
                finishedAt = finishedAt,
                status = SurveyStatus.IN_PROGRESS,
                finishMessage = "b",
                targetParticipants = 200,
                rewards = rewards,
                sections = listOf(section1, section2, section4),
            )
        }
    }
}
