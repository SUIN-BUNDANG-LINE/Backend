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
            rewards: List<Reward>,
            targetParticipantCount: Int?,
            finishedAt: Date?,
        ) = if (rewards.isEmpty()) {
            if (finishedAt == null && targetParticipantCount == null) {
                NoRewardSetting
            } else {
                throw InvalidRewardSettingException()
            }
        } else if (finishedAt == null) {
            throw InvalidRewardSettingException()
        } else if (targetParticipantCount == null) {
            SelfManagementSetting(rewards, FinishedAt(finishedAt))
        } else {
            ImmediateDrawSetting(rewards, targetParticipantCount, FinishedAt(finishedAt))
        }
    }

    fun getRewardCount() = rewards.sumOf { it.count }
}
