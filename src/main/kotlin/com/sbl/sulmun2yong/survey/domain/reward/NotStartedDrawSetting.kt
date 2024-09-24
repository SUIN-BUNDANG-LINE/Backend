package com.sbl.sulmun2yong.survey.domain.reward

/** 시작 전 상태의 불완전한 리워드 지급 설정 */
data class NotStartedDrawSetting(
    override val type: RewardSettingType,
    override val rewards: List<Reward>,
    override val targetParticipantCount: Int?,
    override val finishedAt: FinishedAt?,
) : RewardSetting
