package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.createMockSection
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.FINISHED_AT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.FINISH_MESSAGE
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.PUBLISHED_AT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.REWARDS
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.REWARD_COUNT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.SECTIONS
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.SURVEY_STATUS
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.TARGET_PARTICIPANT_COUNT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.THUMBNAIL
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.createSurvey
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidUpdateSurveyException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals

class SurveyTest {
    private val id = UUID.randomUUID()
    private val visitorId = "abcedfg"

    @Test
    fun `설문의 응답을 생성하면 정보들이 설정된다`() {
        // given
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val id = UUID.randomUUID()
        val sectionResponse1 = SectionResponse(sectionId, listOf())

        // when
        val surveyResponse = SurveyResponse(id, visitorId, listOf(sectionResponse1))

        // then
        assertEquals(id, surveyResponse.surveyId)
        assertEquals(listOf(sectionResponse1), surveyResponse)
    }

    @Test
    fun `설문의 응답은 중복될 수 없다`() {
        // given
        val id = SectionId.Standard(UUID.randomUUID())
        val sectionResponse1 = SectionResponse(id, listOf())
        val sectionResponse2 = SectionResponse(id, listOf())

        // when, then
        assertThrows<InvalidSurveyResponseException> {
            SurveyResponse(UUID.randomUUID(), visitorId, listOf(sectionResponse1, sectionResponse2))
        }
    }

    @Test
    fun `설문을 생성하면 설문의 정보들이 설정된다`() {
        // given, when
        val makerId: UUID = UUID.randomUUID()
        val survey = createSurvey(id = id, makerId = makerId)
        val defaultSurvey = Survey.create(makerId)

        // then
        with(survey) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(THUMBNAIL + id, this.thumbnail)
            assertEquals(FINISHED_AT, this.finishedAt)
            assertEquals(PUBLISHED_AT, this.publishedAt)
            assertEquals(SURVEY_STATUS, this.status)
            assertEquals(FINISH_MESSAGE + id, this.finishMessage)
            assertEquals(TARGET_PARTICIPANT_COUNT, this.targetParticipantCount)
            assertEquals(makerId, this.makerId)
            assertEquals(REWARDS, this.rewards)
            assertEquals(SECTIONS, this.sections)
        }

        val finishDate =
            Date.from(
                LocalDateTime
                    .now()
                    .plusDays(Survey.DEFAULT_SURVEY_DURATION)
                    .withSecond(0)
                    .withNano(0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            )

        with(defaultSurvey) {
            assertEquals(Survey.DEFAULT_TITLE, this.title)
            assertEquals(Survey.DEFAULT_DESCRIPTION, this.description)
            assertEquals(null, this.thumbnail)
            assertEquals(finishDate, this.finishedAt)
            assertEquals(null, this.publishedAt)
            assertEquals(SurveyStatus.NOT_STARTED, this.status)
            assertEquals(Survey.DEFAULT_FINISH_MESSAGE, this.finishMessage)
            assertEquals(Survey.DEFAULT_TARGET_PARTICIPANT_COUNT, this.targetParticipantCount)
            assertEquals(makerId, this.makerId)
            assertEquals(emptyList(), this.rewards)
            assertEquals(listOf(this.sections.first()), this.sections)
        }
    }

    @Test
    fun `설문을 생성할 때 섹션이 1개 이상 없으면 예외가 발생한다`() {
        assertThrows<InvalidSurveyException> { createSurvey(sections = listOf()) }
    }

    @Test
    fun `설문을 생성할 때 섹션들의 ID가 중복되면 예외가 발생한다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()
        val sectionIds = listOf(sectionId1, sectionId2, sectionId3)

        val section1 = createMockSection(sectionId1, SectionId.Standard(sectionId2), sectionIds)
        val section2 = createMockSection(sectionId2, SectionId.Standard(sectionId3), sectionIds)
        val section3 = createMockSection(sectionId3, SectionId.End, sectionIds)

        // when, then
        assertDoesNotThrow { createSurvey(sections = listOf(section1, section2, section3)) }
        assertThrows<InvalidSurveyException> { createSurvey(sections = listOf(section1, section2, section1)) }
    }

    @Test
    fun `설문의 시작일이 마감일 이후면 예외가 발생한다`() {
        // given
        val publishedAt = Date(FINISHED_AT.time + 24 * 60 * 60 * 1000)

        // when, then
        assertThrows<InvalidSurveyException> { createSurvey(publishedAt = publishedAt) }
    }

    @Test
    fun `설문의 시작일은 설문이 시작 전일 때만 null이다`() {
        assertDoesNotThrow { createSurvey(publishedAt = null, status = SurveyStatus.NOT_STARTED) }
        assertThrows<InvalidSurveyException> { createSurvey(publishedAt = null, status = SurveyStatus.IN_PROGRESS) }
        assertThrows<InvalidSurveyException> { createSurvey(publishedAt = null, status = SurveyStatus.IN_MODIFICATION) }
        assertThrows<InvalidSurveyException> { createSurvey(publishedAt = null, status = SurveyStatus.CLOSED) }
    }

    @Test
    fun `설문은 응답의 섹션 순서가 유효한지 검증할 수 있다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()
        val sectionIds = listOf(sectionId1, sectionId2, sectionId3)

        val section1 = createMockSection(sectionId1, SectionId.Standard(sectionId2), sectionIds)
        val section2 = createMockSection(sectionId2, SectionId.Standard(sectionId3), sectionIds)
        val section3 = createMockSection(sectionId3, SectionId.End, sectionIds)

        val id = UUID.randomUUID()
        val survey = createSurvey(id = id, sections = listOf(section1, section2, section3))

        val surveyResponse1 =
            SurveyResponse(
                id,
                visitorId,
                listOf(
                    SectionResponse(SectionId.Standard(sectionId1), listOf()),
                    SectionResponse(SectionId.Standard(sectionId2), listOf()),
                    SectionResponse(SectionId.Standard(sectionId3), listOf()),
                ),
            )

        val surveyResponse2 =
            SurveyResponse(
                id,
                visitorId,
                listOf(
                    SectionResponse(SectionId.Standard(sectionId1), listOf()),
                    SectionResponse(SectionId.Standard(sectionId2), listOf()),
                    SectionResponse(SectionId.Standard(UUID.randomUUID()), listOf()),
                ),
            )

        val surveyResponse3 =
            SurveyResponse(
                id,
                visitorId,
                listOf(
                    SectionResponse(SectionId.Standard(sectionId1), listOf()),
                    SectionResponse(SectionId.Standard(sectionId2), listOf()),
                ),
            )

        // when, then
        assertDoesNotThrow { survey.validateResponse(surveyResponse1) }
        // 잘못된 섹션 ID로 응답을 보낸 경우
        assertThrows<InvalidSurveyResponseException> { survey.validateResponse(surveyResponse2) }
        // 마지막 섹션을 응답하지 않은 경우
        assertThrows<InvalidSurveyResponseException> { survey.validateResponse(surveyResponse3) }
    }

    @Test
    fun `설문은 설문의 리워드 개수를 계산할 수 있다`() {
        // given
        val survey = createSurvey()

        // when
        val count = survey.getRewardCount()

        // then
        assertEquals(REWARD_COUNT, count)
    }

    @Test
    fun `설문의 목표 참여자 수는 설문의 리워드 개수 이상이다`() {
        assertThrows<InvalidSurveyException> { createSurvey(targetParticipantCount = REWARD_COUNT - 1) }
    }

    @Test
    fun `각 섹션의 sectionIds는 설문의 섹션 ID들과 같다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()
        val sectionId4 = UUID.randomUUID()
        val sectionIds = listOf(sectionId1, sectionId2, sectionId3)

        val section1 = createMockSection(sectionId1, SectionId.Standard(sectionId2), sectionIds)
        val section2 = createMockSection(sectionId2, SectionId.Standard(sectionId3), sectionIds)
        val section3 = createMockSection(sectionId3, SectionId.End, sectionIds)
        val section4 = createMockSection(sectionId4, SectionId.End, sectionIds)

        // when, then
        assertDoesNotThrow { createSurvey(sections = listOf(section1, section2, section3)) }

        assertThrows<InvalidSurveyException> { createSurvey(sections = listOf(section1, section2, section4)) }
    }

    @Test
    fun `설문은 종료 상태로 변경할 수 있다`() {
        // given
        val survey = createSurvey()

        // when
        val finishedSurvey = survey.finish()

        // then
        assertEquals(SurveyStatus.CLOSED, finishedSurvey.status)
    }

    @Test
    fun `설문의 내용를 업데이트할 수 있다`() {
        // given
        val newTitle = "new title"
        val newDescription = "new description"
        val newThumbnail = "new thumbnail"
        val newFinishMessage = "new finish message"
        val newTargetParticipantCount = 10
        val newRewards = listOf(Reward("new reward", "new category", 1))
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val newSections =
            listOf(
                Section(
                    id = sectionId,
                    title = "",
                    description = "",
                    routingStrategy = RoutingStrategy.NumericalOrder,
                    questions = emptyList(),
                    sectionIds = SectionIds(listOf(sectionId, SectionId.End)),
                ),
            )
        val survey = createSurvey(status = SurveyStatus.NOT_STARTED)

        // when
        val newSurvey =
            survey.updateContent(
                title = newTitle,
                description = newDescription,
                thumbnail = newThumbnail,
                finishedAt = survey.finishedAt,
                finishMessage = newFinishMessage,
                targetParticipantCount = newTargetParticipantCount,
                rewards = newRewards,
                sections =
                    listOf(
                        Section(
                            id = sectionId,
                            title = "",
                            description = "",
                            routingStrategy = RoutingStrategy.NumericalOrder,
                            questions = emptyList(),
                            sectionIds = SectionIds.from(listOf(sectionId)),
                        ),
                    ),
            )

        // then
        with(newSurvey) {
            assertEquals(newTitle, this.title)
            assertEquals(newDescription, this.description)
            assertEquals(newThumbnail, this.thumbnail)
            assertEquals(survey.finishedAt, this.finishedAt)
            assertEquals(newFinishMessage, this.finishMessage)
            assertEquals(newTargetParticipantCount, this.targetParticipantCount)
            assertEquals(newRewards, this.rewards)
            assertEquals(newSections, this.sections)
        }
    }

    @Test
    fun `설문이 시작 전 상태이거나, 수정 중이면서 리워드 관련 정보가 변경되지 않아야 설문 정보 갱신이 가능하다`() {
        // given
        val survey1 = createSurvey(status = SurveyStatus.IN_PROGRESS)
        val survey2 = createSurvey(status = SurveyStatus.CLOSED)
        val survey3 = createSurvey(status = SurveyStatus.IN_MODIFICATION)

        // when, then
        // 설문이 진행 중인 경우 예외 발생
        assertThrows<InvalidUpdateSurveyException> {
            survey1.updateContent(
                title = survey1.title,
                description = survey1.description,
                thumbnail = survey1.thumbnail,
                finishedAt = survey1.finishedAt,
                finishMessage = survey1.finishMessage,
                targetParticipantCount = survey1.targetParticipantCount,
                rewards = survey1.rewards,
                sections = survey1.sections,
            )
        }
        // 설문이 마감된 경우 예외 발생
        assertThrows<InvalidUpdateSurveyException> {
            survey2.updateContent(
                title = survey2.title,
                description = survey2.description,
                thumbnail = survey2.thumbnail,
                finishedAt = survey2.finishedAt,
                finishMessage = survey2.finishMessage,
                targetParticipantCount = survey2.targetParticipantCount,
                rewards = survey2.rewards,
                sections = survey2.sections,
            )
        }
        // 설문이 수정 중일 때 리워드 관련 정보가 변경되지 않은 경우 정상적으로 진행
        assertDoesNotThrow {
            survey3.updateContent(
                title = survey3.title,
                description = survey3.description,
                thumbnail = survey3.thumbnail,
                finishedAt = survey3.finishedAt,
                finishMessage = survey3.finishMessage,
                targetParticipantCount = survey3.targetParticipantCount,
                rewards = survey3.rewards,
                sections = survey3.sections,
            )
        }
        // 설문이 수정 중일 때 리워드 관련 정보가 변경된 경우 예외 발생
        assertThrows<InvalidUpdateSurveyException> {
            survey3.updateContent(
                title = survey3.title,
                description = survey3.description,
                thumbnail = survey3.thumbnail,
                finishedAt = survey3.finishedAt,
                finishMessage = survey3.finishMessage,
                targetParticipantCount = survey3.targetParticipantCount,
                rewards = listOf(),
                sections = survey3.sections,
            )
        }
        assertThrows<InvalidUpdateSurveyException> {
            survey3.updateContent(
                title = survey3.title,
                description = survey3.description,
                thumbnail = survey3.thumbnail,
                finishedAt = survey3.finishedAt,
                finishMessage = survey3.finishMessage,
                targetParticipantCount = 1000,
                rewards = survey3.rewards,
                sections = survey3.sections,
            )
        }
    }
}
