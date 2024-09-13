package com.sbl.sulmun2yong.survey.domain.reward

/** 설문의 리워드와 관련된 정보를 담고있는 클래스 */
interface RewardInfo {
    val rewards: List<Reward>
    val targetParticipantCount: Int?
    val isImmediateDraw: Boolean

    companion object {
        fun of(
            rewards: List<Reward>,
            targetParticipantCount: Int?,
        ) = if (targetParticipantCount != null) ImmediateDrawRewardInfo(rewards, targetParticipantCount) else ByUserRewardInfo(rewards)
    }

    fun getRewardCount() = rewards.sumOf { it.count }
}