package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import java.util.Date

data class SurveyInfoResponse(
    val title: String,
    val description: String,
    val status: SurveyStatus,
    val finishedAt: Date,
    val currentParticipants: Int,
    val targetParticipants: Int,
    val thumbnail: String,
    val rewards: List<RewardInfoResponse>,
) {
    companion object {
        fun of(survey: Survey) =
            SurveyInfoResponse(
                title = survey.title,
                description = survey.description,
                status = survey.status,
                finishedAt = survey.finishedAt,
                // TODO: 실제 현재 참여자 값 넣도록 구현하기
                currentParticipants = 0,
                targetParticipants = survey.targetParticipantCount,
                thumbnail = survey.thumbnail,
                rewards = survey.rewards.map { it.toResponse() },
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
