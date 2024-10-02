package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.Date
import java.util.UUID

object SurveyFixtureFactory {
    const val TITLE = "설문 제목"
    const val DESCRIPTION = "설문 설명"
    const val THUMBNAIL = "설문 썸네일"
    val SURVEY_STATUS = SurveyStatus.IN_PROGRESS
    val FINISHED_AT = DateUtil.getCurrentDate(noMin = true)
    val PUBLISHED_AT = Date(FINISHED_AT.time - 24 * 60 * 60 * 10000)
    const val FINISH_MESSAGE = "설문이 종료되었습니다."
    const val TARGET_PARTICIPANT_COUNT = 100
    val REWARDS =
        listOf(
            Reward("아메리카노", "커피", 3),
            Reward("카페라떼", "커피", 2),
            Reward("햄버거", "음식", 4),
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

    fun createSurvey(
        id: UUID = UUID.randomUUID(),
        title: String = TITLE,
        description: String = DESCRIPTION,
        thumbnail: String = THUMBNAIL,
        publishedAt: Date? = PUBLISHED_AT,
        finishedAt: Date? = FINISHED_AT,
        status: SurveyStatus = SURVEY_STATUS,
        finishMessage: String = FINISH_MESSAGE,
        targetParticipantCount: Int? = TARGET_PARTICIPANT_COUNT,
        type: RewardSettingType = RewardSettingType.IMMEDIATE_DRAW,
        makerId: UUID = UUID.randomUUID(),
        rewards: List<Reward> = REWARDS,
        isVisible: Boolean = true,
        sections: List<Section> = SECTIONS,
    ) = Survey(
        id = id,
        title = title + id,
        description = description + id,
        thumbnail = thumbnail + id,
        publishedAt = publishedAt,
        status = status,
        finishMessage = finishMessage + id,
        makerId = makerId,
        rewardSetting = createRewardSetting(type, rewards, targetParticipantCount, finishedAt, status),
        isVisible = isVisible,
        sections = sections,
    )

    fun createRewardSetting(
        type: RewardSettingType = RewardSettingType.IMMEDIATE_DRAW,
        rewards: List<Reward> = REWARDS,
        targetParticipantCount: Int? = TARGET_PARTICIPANT_COUNT,
        finishedAt: Date? = FINISHED_AT,
        status: SurveyStatus = SURVEY_STATUS,
    ) = RewardSetting.of(type, rewards, targetParticipantCount, finishedAt, status)
}
