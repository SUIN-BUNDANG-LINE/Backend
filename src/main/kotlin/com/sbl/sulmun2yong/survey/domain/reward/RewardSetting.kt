package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.InvalidRewardSettingException
import java.util.Date

/** 설문의 리워드 지급에 대한 설정을 담고있는 클래스 */
interface RewardSetting {
    val type: RewardSettingType
    val rewards: List<Reward>
    val targetParticipantCount: Int?
    val finishedAt: FinishedAt?
    val isImmediateDraw: Boolean
        get() = type == RewardSettingType.IMMEDIATE_DRAW

    companion object {
        fun of(
            type: RewardSettingType,
            rewards: List<Reward>,
            targetParticipantCount: Int?,
            finishedAt: Date?,
            surveyStatus: SurveyStatus = SurveyStatus.IN_PROGRESS,
        ): RewardSetting {
            if (surveyStatus == SurveyStatus.NOT_STARTED) {
                val finishedAtValue = finishedAt?.let { FinishedAt(it) }
                return NotStartedDrawSetting(type, rewards, targetParticipantCount, finishedAtValue)
            }
            when (type) {
                RewardSettingType.NO_REWARD -> {
                    val isNoReward = rewards.isEmpty() && targetParticipantCount == null && finishedAt == null
                    if (isNoReward) return NoRewardSetting
                    throw InvalidRewardSettingException()
                }
                RewardSettingType.SELF_MANAGEMENT -> {
                    val isSelfManagement = rewards.isNotEmpty() && targetParticipantCount == null && finishedAt != null
                    if (isSelfManagement) return SelfManagementSetting(rewards, FinishedAt(finishedAt!!))
                    throw InvalidRewardSettingException()
                }
                RewardSettingType.IMMEDIATE_DRAW -> {
                    val isImmediateDraw = rewards.isNotEmpty() && targetParticipantCount != null && finishedAt != null
                    if (isImmediateDraw) return ImmediateDrawSetting(rewards, targetParticipantCount!!, FinishedAt(finishedAt!!))
                    throw InvalidRewardSettingException()
                }
            }
        }
    }

    fun getRewardCount() = rewards.sumOf { it.count }
}
