package com.sbl.sulmun2yong.survey.domain

import java.util.Date
import java.util.UUID

data class Survey(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String,
    val endDate: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipants: Int?,
    val rewards: List<Reward>,
    val createdAt: Date,
    val updatedAt: Date,
) {
    fun getRewardCount(): Int {
        return rewards.sumOf { it.count }
    }
}
