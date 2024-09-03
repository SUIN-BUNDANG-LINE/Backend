package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidDrawTypeException

/** 설문의 추첨 방식과 관련된 정보를 담고있는 클래스 */
sealed class DrawType {
    abstract val rewards: List<Reward>
    abstract val targetParticipantCount: Int?

    companion object {
        fun of(
            rewards: List<Reward>,
            targetParticipantCount: Int?,
        ) = if (targetParticipantCount != null) Immediate(rewards, targetParticipantCount) else Free(rewards)
    }

    fun getRewardCount() = rewards.sumOf { it.count }

    /** 즉시 추첨(설문 참여 후 추첨 보드를 통해 즉시 추첨 진행) */
    data class Immediate(
        override val rewards: List<Reward>,
        override val targetParticipantCount: Int,
    ) : DrawType() {
        init {
            require(rewards.isNotEmpty()) { throw InvalidDrawTypeException() }
            // 즉시 추첨은 리워드 개수의 총합이 목표 참여자 수보다 적어야함
            require(isTargetParticipantValid()) { throw InvalidDrawTypeException() }
        }

        private fun isTargetParticipantValid() = targetParticipantCount >= getRewardCount()
    }

    /** 자유(사용자가 직접 추첨 or 추첨을 진행하지 않음) */
    data class Free(
        override val rewards: List<Reward>,
    ) : DrawType() {
        override val targetParticipantCount = null
    }
}
