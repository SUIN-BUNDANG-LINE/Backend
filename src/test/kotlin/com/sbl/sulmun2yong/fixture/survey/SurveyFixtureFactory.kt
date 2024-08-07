package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.time.Instant
import java.util.Date
import java.util.UUID

object SurveyFixtureFactory {
    const val TITLE = "설문 제목"
    const val DESCRIPTION = "설문 설명"
    const val THUMBNAIL = "설문 썸네일"
    val SURVEY_STATUS = SurveyStatus.IN_PROGRESS
    val FINISHED_AT = Date.from(Instant.now())!!
    val PUBLISHED_AT = Date(FINISHED_AT.time - 24 * 60 * 60 * 10000)
    const val FINISH_MESSAGE = "설문이 종료되었습니다."
    const val TARGET_PARTICIPANT_COUNT = 100
    val REWARDS =
        listOf(
            Reward(UUID.randomUUID(), "아메리카노", "커피", 3),
            Reward(UUID.randomUUID(), "카페라떼", "커피", 2),
            Reward(UUID.randomUUID(), "햄버거", "음식", 4),
        )
    val SECTIONS =
        let {
            val id = SectionId.Standard(UUID.randomUUID())
            listOf(
                Section(
                    id = id,
                    title = "",
                    description = "",
                    routingStrategy = RoutingStrategy.NumericalOrder,
                    questions = emptyList(),
                    sectionIds = SectionIds(listOf(id, SectionId.End)),
                ),
            )
        }
    const val REWARD_COUNT = 9

    fun createSurvey(
        id: UUID = UUID.randomUUID(),
        title: String = TITLE,
        description: String = DESCRIPTION,
        thumbnail: String = THUMBNAIL,
        publishedAt: Date? = PUBLISHED_AT,
        finishedAt: Date = FINISHED_AT,
        status: SurveyStatus = SURVEY_STATUS,
        finishMessage: String = FINISH_MESSAGE,
        targetParticipantCount: Int = TARGET_PARTICIPANT_COUNT,
        makerId: UUID = UUID.randomUUID(),
        rewards: List<Reward> = REWARDS,
        sections: List<Section> = SECTIONS,
    ) = Survey(
        id = id,
        title = title + id,
        description = description + id,
        thumbnail = thumbnail + id,
        publishedAt = publishedAt,
        finishedAt = finishedAt,
        status = status,
        finishMessage = finishMessage + id,
        targetParticipantCount = targetParticipantCount,
        makerId = makerId,
        rewards = rewards,
        sections = sections,
    )
}
