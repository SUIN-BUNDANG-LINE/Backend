package com.sbl.sulmun2yong.user.util

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.FinishedAt
import com.sbl.sulmun2yong.survey.domain.reward.NoRewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.reward.SelfManagementSetting
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

object RandomSurveyGenerateUtil {
    private val randomSurveyStatusPicker =
        ProbabilityPicker(
            mapOf(
                SurveyStatus.NOT_STARTED to 0.1,
                SurveyStatus.IN_PROGRESS to 0.45,
                SurveyStatus.IN_MODIFICATION to 0.05,
                SurveyStatus.CLOSED to 0.4,
            ),
        )

    private val randomSectionCountPicker =
        ProbabilityPicker(
            mapOf(
                1 to 0.15,
                2 to 0.25,
                3 to 0.35,
                4 to 0.2,
                5 to 0.05,
            ),
        )

    private val randomRewardSettingTypePicker =
        ProbabilityPicker(
            mapOf(
                RewardSettingType.NO_REWARD to 0.5,
                RewardSettingType.SELF_MANAGEMENT to 0.5,
                // 편의상 즉시 추첨은 제외
            ),
        )

    private val randomIsVisible = Probability(0.7)
    private val randomIsResultOpen = Probability(0.5)

    private val randomRewardPicker =
        ProbabilityPicker(
            mapOf(
                Reward("스타벅스 아메리카노 T", "커피", 10) to 0.25,
                Reward("스타벅스 카페라떼 T", "커피", 10) to 0.25,
                Reward("BBQ 황금올리브", "치킨", 10) to 0.25,
                Reward("BHC 맛초킹", "치킨", 10) to 0.25,
            ),
        )

    fun generateRandomSurvey(makerId: UUID): Survey {
        val id = UUID.randomUUID()
        val title = "설문 $id"
        val description = "설문 $id 설명"
        val surveyStatus = randomSurveyStatusPicker.pick()
        val finishMessage = "설문 $id 종료 메시지"
        val isVisible = randomIsVisible.isWinning()
        val isResultOpen = randomIsResultOpen.isWinning()
        val sections = getRandomSections()
        val publishedAt =
            when (surveyStatus) {
                SurveyStatus.NOT_STARTED -> null
                else -> RandomUtil.getRandomDate(-40, -30)
            }
        val rewardSetting =
            when (randomRewardSettingTypePicker.pick()) {
                RewardSettingType.NO_REWARD -> NoRewardSetting
                RewardSettingType.SELF_MANAGEMENT ->
                    SelfManagementSetting(
                        rewards =
                            listOf(
                                randomRewardPicker.pick(),
                            ),
                        finishedAt =
                            when (surveyStatus) {
                                SurveyStatus.NOT_STARTED -> FinishedAt(RandomUtil.getRandomDate(30, 40))
                                SurveyStatus.CLOSED -> FinishedAt(RandomUtil.getRandomDate(-20, -10))
                                else -> FinishedAt(RandomUtil.getRandomDate(10, 20))
                            },
                    )

                else -> throw IllegalArgumentException("즉시 추첨 방식은 지원되지 않습니다.")
            }

        return Survey(
            id = id,
            title = title,
            description = description,
            // 편의상 null로 고정
            thumbnail = null,
            publishedAt = publishedAt,
            status = surveyStatus,
            finishMessage = finishMessage,
            rewardSetting = rewardSetting,
            isVisible = isVisible,
            makerId = makerId,
            isResultOpen = isResultOpen,
            sections = sections,
        )
    }

    private fun getRandomSections(): List<Section> {
        val sectionCount = randomSectionCountPicker.pick()
        val sectionIdList = (0 until sectionCount).map { SectionId.Standard(UUID.randomUUID()) }
        val sectionIds = SectionIds.from(sectionIdList)
        return sectionIdList.mapIndexed { index, sectionId ->
            RandomSectionGenerateUtil.generateRandomSection(index, sectionId, sectionIds)
        }
    }
}
