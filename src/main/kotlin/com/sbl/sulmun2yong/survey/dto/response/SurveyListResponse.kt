package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Survey
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
                            targetParticipants = it.targetParticipantCount,
                            finishedAt = it.finishedAt,
                            rewardCount = it.getRewardCount(),
                            rewards = it.rewards.toRewardInfoResponses(),
                        )
                    },
            )
    }

    data class SurveyInfoResponse(
        val surveyId: UUID,
        val thumbnail: String,
        val title: String,
        val description: String,
        val targetParticipants: Int,
        val rewardCount: Int,
        val finishedAt: Date,
        val rewards: List<RewardInfoResponse>,
    )

    data class RewardInfoResponse(
        val category: String,
        val items: List<String>,
    )
}

private fun List<Reward>.toRewardInfoResponses(): List<SurveyListResponse.RewardInfoResponse> =
    this.groupBy { it.category }
        .map { (category, rewardsInCategory) ->
            SurveyListResponse.RewardInfoResponse(
                category = category,
                items = rewardsInCategory.map { it.name },
            )
        }
