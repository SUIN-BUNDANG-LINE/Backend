package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import java.util.Date
import java.util.UUID

data class SurveyListResponse(
    val pageCount: Int,
    val surveys: List<SurveyInfoResponse>,
) {
    companion object {
        fun of(
            pageCount: Int,
            surveys: List<Survey>,
        ): SurveyListResponse =
            SurveyListResponse(
                pageCount = pageCount,
                surveys =
                    surveys.map {
                        SurveyInfoResponse(
                            surveyId = it.id,
                            thumbnail = it.thumbnail,
                            title = it.title,
                            description = it.description,
                            targetParticipants = it.rewardSetting.targetParticipantCount,
                            finishedAt = it.rewardSetting.finishedAt?.value,
                            rewardCount = it.rewardSetting.getRewardCount(),
                            rewardSettingType = it.rewardSetting.type,
                            rewards = it.rewardSetting.rewards.toRewardInfoResponses(),
                            isResultOpen = it.isResultOpen,
                        )
                    },
            )
    }

    data class SurveyInfoResponse(
        val surveyId: UUID,
        val thumbnail: String?,
        val title: String,
        val description: String,
        val targetParticipants: Int?,
        val rewardCount: Int,
        val finishedAt: Date?,
        val rewardSettingType: RewardSettingType,
        val rewards: List<RewardInfoResponse>,
        val isResultOpen: Boolean,
    )

    data class RewardInfoResponse(
        val category: String,
        val items: List<String>,
    )
}

private fun List<Reward>.toRewardInfoResponses(): List<SurveyListResponse.RewardInfoResponse> =
    this
        .groupBy { it.category }
        .map { (category, rewardsInCategory) ->
            SurveyListResponse.RewardInfoResponse(
                category = category,
                items = rewardsInCategory.map { it.name },
            )
        }
