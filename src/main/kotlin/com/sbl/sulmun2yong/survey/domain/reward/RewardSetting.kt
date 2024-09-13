package com.sbl.sulmun2yong.survey.domain.reward

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
        ) = when (type) {
            RewardSettingType.NO_REWARD -> {
                if (rewards.isEmpty() && targetParticipantCount == null && finishedAt == null) {
                    NoRewardSetting
                } else {
                    throw InvalidRewardSettingException()
                }
            }
            RewardSettingType.SELF_MANAGEMENT -> {
                if (rewards.isNotEmpty() &&
                    targetParticipantCount == null &&
                    finishedAt != null
                ) {
                    SelfManagementSetting(rewards, FinishedAt(finishedAt))
                } else {
                    throw InvalidRewardSettingException()
                }
            }
            RewardSettingType.IMMEDIATE_DRAW -> {
                if (rewards.isNotEmpty() &&
                    targetParticipantCount != null &&
                    finishedAt != null
                ) {
                    ImmediateDrawSetting(rewards, targetParticipantCount, FinishedAt(finishedAt))
                } else {
                    throw InvalidRewardSettingException()
                }
            }
        }
    }

    fun getRewardCount() = rewards.sumOf { it.count }
}
