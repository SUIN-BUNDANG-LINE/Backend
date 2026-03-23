package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "surveys")
class SurveyEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(length = 500)
    val thumbnail: String?,
    val publishedAt: Date?,
    val finishedAt: Date?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: SurveyStatus,
    @Column(nullable = false, columnDefinition = "TEXT")
    val finishMessage: String,
    val targetParticipantCount: Int?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val rewardSettingType: RewardSettingType,
    @Column(nullable = false)
    val isVisible: Boolean,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val makerId: UUID,
    @Column(nullable = false)
    val isResultOpen: Boolean,
    @OneToMany(mappedBy = "survey", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val rewards: MutableList<RewardEntity> = mutableListOf(),
    @OneToMany(mappedBy = "survey", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val sections: MutableList<SectionEntity> = mutableListOf(),
    @Column(nullable = false)
    val isDeleted: Boolean = false,
) : BaseTimeEntity() {
    companion object {
        fun from(survey: Survey): SurveyEntity {
            val entity =
                SurveyEntity(
                    id = survey.id,
                    title = survey.title,
                    description = survey.description,
                    thumbnail = survey.thumbnail,
                    publishedAt = survey.publishedAt,
                    finishedAt = survey.rewardSetting.finishedAt?.value,
                    status = survey.status,
                    finishMessage = survey.finishMessage,
                    targetParticipantCount = survey.rewardSetting.targetParticipantCount,
                    rewardSettingType = survey.rewardSetting.type,
                    isVisible = survey.isVisible,
                    makerId = survey.makerId,
                    isResultOpen = survey.isResultOpen,
                )
            // 양방향 관계 설정
            survey.rewardSetting.rewards.forEachIndexed { index, reward ->
                entity.rewards.add(RewardEntity.from(reward, entity, index))
            }
            survey.sections.forEachIndexed { index, section ->
                entity.sections.add(SectionEntity.from(section, entity, index))
            }
            return entity
        }
    }

    fun toDomain(): Survey {
        val sectionIds =
            if (sections.isEmpty()) {
                SectionIds.from(emptyList())
            } else {
                SectionIds.from(sections.map { SectionId.Standard(it.id) })
            }
        return Survey(
            id = id,
            title = title,
            description = description,
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            status = status,
            finishMessage = finishMessage,
            rewardSetting =
                RewardSetting.of(
                    rewardSettingType,
                    rewards.map { it.toDomain() },
                    targetParticipantCount,
                    finishedAt,
                    status,
                ),
            isVisible = isVisible,
            isResultOpen = isResultOpen,
            makerId = makerId,
            sections = sections.map { it.toDomain(sectionIds) },
        )
    }
}
