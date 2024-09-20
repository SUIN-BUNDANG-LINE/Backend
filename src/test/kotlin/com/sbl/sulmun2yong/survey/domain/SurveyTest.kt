package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.createMockSection
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.FINISHED_AT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.FINISH_MESSAGE
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.PUBLISHED_AT
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.SECTIONS
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.SURVEY_STATUS
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.THUMBNAIL
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.createRewardSetting
import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory.createSurvey
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.reward.NoRewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidPublishedAtException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyStartException
import com.sbl.sulmun2yong.survey.exception.InvalidUpdateSurveyException
import com.sbl.sulmun2yong.survey.exception.SurveyClosedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class SurveyTest {
    private val id = UUID.randomUUID()

    @Test
    fun `설문의 응답을 생성하면 정보들이 설정된다`() {
        // given
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val id = UUID.randomUUID()
        val sectionResponse1 = SectionResponse(sectionId, listOf())

        // when
        val surveyResponse = SurveyResponse(id, listOf(sectionResponse1))

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
            SurveyResponse(UUID.randomUUID(), listOf(sectionResponse1, sectionResponse2))
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
            assertEquals(PUBLISHED_AT, this.publishedAt)
            assertEquals(SURVEY_STATUS, this.status)
            assertEquals(FINISH_MESSAGE + id, this.finishMessage)
            assertEquals(createRewardSetting(), this.rewardSetting)
            assertEquals(true, this.isVisible)
            assertEquals(makerId, this.makerId)
            assertEquals(SECTIONS, this.sections)
        }

        with(defaultSurvey) {
            assertEquals(Survey.DEFAULT_TITLE, this.title)
            assertEquals(Survey.DEFAULT_DESCRIPTION, this.description)
            assertEquals(null, this.thumbnail)
            assertEquals(null, this.publishedAt)
            assertEquals(SurveyStatus.NOT_STARTED, this.status)
            assertEquals(Survey.DEFAULT_FINISH_MESSAGE, this.finishMessage)
            assertEquals(NoRewardSetting, this.rewardSetting)
            assertEquals(true, this.isVisible)
            assertEquals(makerId, this.makerId)
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
        val publishedAtAfterFinishedAt = DateUtil.getDateAfterDay(FINISHED_AT)

        // when, then
        // 아직 시작하지 않은 경우 예외가 발생하지 않는다.
        assertDoesNotThrow {
            createSurvey(
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                targetParticipantCount = null,
                finishedAt = null,
                rewards = emptyList(),
                type = RewardSettingType.NO_REWARD,
            )
        }
        // 리워드 설정이 즉시 추첨인 설문은 시작일이 마감일 이후면 예외가 발생한다.
        assertThrows<InvalidPublishedAtException> { createSurvey(publishedAt = publishedAtAfterFinishedAt) }
        // 리워드 설정이 직접 관리인 설문은 시작일이 마감일 이후면 예외가 발생한다.
        assertThrows<InvalidPublishedAtException> {
            createSurvey(type = RewardSettingType.SELF_MANAGEMENT, publishedAt = publishedAtAfterFinishedAt, targetParticipantCount = null)
        }
        // 리워드 미 지급 설문은 마감일이 존재하지 않으므로 예외가 발생하지 않는다.
        assertDoesNotThrow {
            createSurvey(
                type = RewardSettingType.NO_REWARD,
                publishedAt = publishedAtAfterFinishedAt,
                targetParticipantCount = null,
                finishedAt = null,
                rewards = emptyList(),
            )
        }
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
        val notStartedSurvey =
            createSurvey(
                id = id,
                sections = listOf(section1, section2, section3),
                status = SurveyStatus.NOT_STARTED,
                rewards = emptyList(),
                targetParticipantCount = null,
                finishedAt = null,
                publishedAt = null,
                type = RewardSettingType.NO_REWARD,
            )

        val surveyResponse1 =
            SurveyResponse(
                id,
                listOf(
                    SectionResponse(SectionId.Standard(sectionId1), listOf()),
                    SectionResponse(SectionId.Standard(sectionId2), listOf()),
                    SectionResponse(SectionId.Standard(sectionId3), listOf()),
                ),
            )

        val surveyResponse2 =
            SurveyResponse(
                id,
                listOf(
                    SectionResponse(SectionId.Standard(sectionId1), listOf()),
                    SectionResponse(SectionId.Standard(sectionId2), listOf()),
                    SectionResponse(SectionId.Standard(UUID.randomUUID()), listOf()),
                ),
            )

        val surveyResponse3 =
            SurveyResponse(
                id,
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
        // 설문이 시작되지 않은 경우 예외 발생
        assertThrows<SurveyClosedException> { notStartedSurvey.validateResponse(surveyResponse1) }
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
    fun `진행 중인 설문은 수정 중인 상태로 변경할 수 있다`() {
        // given
        val inProgressSurvey =
            createSurvey(type = RewardSettingType.SELF_MANAGEMENT, status = SurveyStatus.IN_PROGRESS, targetParticipantCount = null)
        val notStartedSurvey =
            createSurvey(type = RewardSettingType.SELF_MANAGEMENT, status = SurveyStatus.NOT_STARTED, targetParticipantCount = null)
        val inModificationSurvey =
            createSurvey(type = RewardSettingType.SELF_MANAGEMENT, status = SurveyStatus.IN_MODIFICATION, targetParticipantCount = null)
        val closedSurvey =
            createSurvey(type = RewardSettingType.SELF_MANAGEMENT, status = SurveyStatus.CLOSED, targetParticipantCount = null)

        // when, then
        assertEquals(SurveyStatus.IN_MODIFICATION, inProgressSurvey.edit().status)
        assertThrows<InvalidSurveyEditException> { notStartedSurvey.edit() }
        assertThrows<InvalidSurveyEditException> { inModificationSurvey.edit() }
        assertThrows<InvalidSurveyEditException> { closedSurvey.edit() }
    }

    @Test
    fun `설문의 내용를 업데이트할 수 있다`() {
        // given
        val newTitle = "new title"
        val newDescription = "new description"
        val newThumbnail = "new thumbnail"
        val newFinishMessage = "new finish message"
        val newRewardSetting =
            RewardSetting.of(
                type = RewardSettingType.IMMEDIATE_DRAW,
                listOf(Reward("new reward", "new category", 1)),
                10,
                DateUtil.getCurrentDate(noMin = true),
            )
        val newIsVisible = false
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
                finishMessage = newFinishMessage,
                rewardSetting = newRewardSetting,
                isVisible = newIsVisible,
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
            assertEquals(newFinishMessage, this.finishMessage)
            assertEquals(newRewardSetting, this.rewardSetting)
            assertEquals(isVisible, this.isVisible)
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
                finishMessage = survey1.finishMessage,
                rewardSetting = survey1.rewardSetting,
                isVisible = survey1.isVisible,
                sections = survey1.sections,
            )
        }
        // 설문이 마감된 경우 예외 발생
        assertThrows<InvalidUpdateSurveyException> {
            survey2.updateContent(
                title = survey2.title,
                description = survey2.description,
                thumbnail = survey2.thumbnail,
                finishMessage = survey2.finishMessage,
                rewardSetting = survey2.rewardSetting,
                isVisible = survey2.isVisible,
                sections = survey2.sections,
            )
        }
        // 설문이 수정 중일 때 리워드 관련 정보가 변경되지 않은 경우 정상적으로 진행
        assertDoesNotThrow {
            survey3.updateContent(
                title = survey3.title,
                description = survey3.description,
                thumbnail = survey3.thumbnail,
                finishMessage = survey3.finishMessage,
                rewardSetting = survey3.rewardSetting,
                isVisible = survey3.isVisible,
                sections = survey3.sections,
            )
        }
        // 설문이 수정 중일 때 리워드 관련 정보가 변경된 경우 예외 발생
        assertThrows<InvalidUpdateSurveyException> {
            survey3.updateContent(
                title = survey3.title,
                description = survey3.description,
                thumbnail = survey3.thumbnail,
                finishMessage = survey3.finishMessage,
                rewardSetting = NoRewardSetting,
                isVisible = survey3.isVisible,
                sections = survey3.sections,
            )
        }
    }

    @Test
    fun `설문을 시작하면, 설문의 시작일과 상태가 업데이트된다`() {
        // given
        val notStartedSurvey =
            createSurvey(
                type = RewardSettingType.SELF_MANAGEMENT,
                finishedAt = DateUtil.getDateAfterDay(date = DateUtil.getCurrentDate(noMin = true)),
                publishedAt = null,
                targetParticipantCount = null,
                status = SurveyStatus.NOT_STARTED,
            )
        val inModificationSurvey =
            createSurvey(
                type = RewardSettingType.SELF_MANAGEMENT,
                finishedAt = DateUtil.getDateAfterDay(date = DateUtil.getCurrentDate(noMin = true)),
                targetParticipantCount = null,
                status = SurveyStatus.IN_MODIFICATION,
            )

        // when
        val startedSurvey1 = notStartedSurvey.start()
        val startedSurvey2 = inModificationSurvey.start()

        // then
        assertEquals(DateUtil.getCurrentDate(), startedSurvey1.publishedAt)
        assertEquals(SurveyStatus.IN_PROGRESS, startedSurvey1.status)
        assertEquals(inModificationSurvey.publishedAt, startedSurvey2.publishedAt)
        assertEquals(SurveyStatus.IN_PROGRESS, startedSurvey2.status)
    }

    @Test
    fun `설문이 시작 전 상태나 수정 중인 상태가 아니면 시작할 수 없다`() {
        // given
        val inProgressSurvey = createSurvey(status = SurveyStatus.IN_PROGRESS)
        val inModificationSurvey = createSurvey(status = SurveyStatus.CLOSED)

        // when, then
        assertThrows<InvalidSurveyStartException> { inProgressSurvey.start() }
        assertThrows<InvalidSurveyStartException> { inModificationSurvey.start() }
    }

    @Test
    fun `설문은 설문의 추첨 방식이 즉시 추첨 방식인지 확인할 수 있다`() {
        // given
        val survey1 =
            createSurvey(
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
            )
        val survey2 =
            createSurvey(
                finishedAt = DateUtil.getDateAfterDay(date = DateUtil.getCurrentDate(noMin = true)),
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                targetParticipantCount = null,
                type = RewardSettingType.SELF_MANAGEMENT,
            )

        // when
        val isImmediateDraw1 = survey1.isImmediateDraw()
        val isImmediateDraw2 = survey2.isImmediateDraw()

        // then
        assertEquals(true, isImmediateDraw1)
        assertEquals(false, isImmediateDraw2)
    }
}
