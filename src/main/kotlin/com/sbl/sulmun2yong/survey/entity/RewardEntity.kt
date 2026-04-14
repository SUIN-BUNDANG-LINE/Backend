package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.survey.domain.reward.Reward
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "rewards")
class RewardEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    val survey: Survey,
    @Column(nullable = false)
    val orderIndex: Int,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val category: String,
    @Column(nullable = false)
    val count: Int,
) {
    companion object {
        fun from(
            reward: Reward,
            survey: Survey,
            orderIndex: Int,
        ) = RewardEntity(
            survey = survey,
            orderIndex = orderIndex,
            name = reward.name,
            category = reward.category,
            count = reward.count,
        )
    }

    fun toDomain() =
        Reward(
            name = name,
            category = category,
            count = count,
        )
}
