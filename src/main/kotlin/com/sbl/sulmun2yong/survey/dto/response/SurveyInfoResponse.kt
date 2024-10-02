package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import java.util.Date

data class SurveyInfoResponse(
    val title: String,
    val description: String,
    val status: SurveyStatus,
    val type: RewardSettingType,
    val finishedAt: Date?,
    val currentParticipants: Int?,
    val targetParticipants: Int?,
    val rewards: List<RewardInfoResponse>,
    val thumbnail: String?,
) {
    companion object {
        fun of(
            survey: Survey,
            currentParticipants: Int?,
        ) = SurveyInfoResponse(
            title = survey.title,
            description = survey.description,
            status = survey.status,
            type = survey.rewardSetting.type,
            finishedAt = survey.rewardSetting.finishedAt?.value,
            currentParticipants = currentParticipants,
            targetParticipants = survey.rewardSetting.targetParticipantCount,
            rewards = survey.rewardSetting.rewards.map { it.toResponse() },
            thumbnail = survey.thumbnail,
        )
    }

    data class RewardInfoResponse(
        val item: String,
        val count: Int,
    )
}

private fun Reward.toResponse() =
    SurveyInfoResponse.RewardInfoResponse(
        item = this.name,
        count = this.count,
    )
