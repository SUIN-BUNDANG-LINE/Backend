package com.sbl.sulmun2yong.survey.domain.reward

/** 사용자 설정(사용자가 직접 추첨 or 추첨을 진행하지 않음) */
data class ByUserRewardInfo(
    override val rewards: List<Reward>,
) : RewardInfo {
    override val targetParticipantCount = null
    override val isImmediateDraw = false
}
