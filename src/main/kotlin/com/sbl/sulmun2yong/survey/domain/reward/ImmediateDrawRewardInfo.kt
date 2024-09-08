package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidRewardInfoException

/** 즉시 추첨(설문 참여 후 추첨 보드를 통해 즉시 추첨 진행) */
data class ImmediateDrawRewardInfo(
    override val rewards: List<Reward>,
    override val targetParticipantCount: Int,
) : RewardInfo {
    override val isImmediateDraw = true

    init {
        require(rewards.isNotEmpty()) { throw InvalidRewardInfoException() }
        // 즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야함
        require(isTargetParticipantValid()) { throw InvalidRewardInfoException() }
    }

    private fun isTargetParticipantValid() = targetParticipantCount >= getRewardCount()
}