package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidRewardSettingException

/** 직접 관리(사용자가 직접 리워드 지급) */
data class SelfManagementSetting(
    override val rewards: List<Reward>,
    override val finishedAt: FinishedAt,
) : RewardSetting {
    override val type = RewardSettingType.SELF_MANAGEMENT
    override val targetParticipantCount = null

    init {
        require(rewards.isNotEmpty()) { throw InvalidRewardSettingException() }
    }
}
