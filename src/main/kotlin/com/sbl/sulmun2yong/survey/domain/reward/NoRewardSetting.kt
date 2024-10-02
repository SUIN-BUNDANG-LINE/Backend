package com.sbl.sulmun2yong.survey.domain.reward

/** 직접 관리(사용자가 직접 리워드 지급) */
object NoRewardSetting : RewardSetting {
    override val rewards = emptyList<Reward>()
    override val finishedAt = null
    override val type = RewardSettingType.NO_REWARD
    override val targetParticipantCount = null
}
