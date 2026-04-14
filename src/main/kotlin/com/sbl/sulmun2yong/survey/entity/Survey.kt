package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.InvalidSurveyEditException
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.reward.NoRewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyStartException
import com.sbl.sulmun2yong.survey.exception.InvalidUpdateSurveyException
import com.sbl.sulmun2yong.survey.exception.SurveyClosedException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "surveys")
class Survey(
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
    val rewardEntities: MutableList<RewardEntity> = mutableListOf(),
    @OneToMany(mappedBy = "survey", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val sectionEntities: MutableList<SectionEntity> = mutableListOf(),
    @Column(nullable = false)
    val isDeleted: Boolean = false,
) : BaseTimeEntity() {
    @get:Transient
    val rewardSetting: RewardSetting
        get() =
            RewardSetting.of(
                rewardSettingType,
                rewardEntities.map { it.toDomain() },
                targetParticipantCount,
                finishedAt,
                status,
            )

    @get:Transient
    val sections: List<Section>
        get() {
            val sectionIds =
                if (sectionEntities.isEmpty()) {
                    SectionIds.from(emptyList())
                } else {
                    SectionIds.from(sectionEntities.map { SectionId.Standard(it.id) })
                }
            return sectionEntities.map { it.toDomain(sectionIds) }
        }

    fun validateResponse(surveyResponse: SurveyResponse) {
        require(status == SurveyStatus.IN_PROGRESS) { throw SurveyClosedException() }
        var expectedSectionId: SectionId = sections.first().id
        for (sectionResponse in surveyResponse) {
            val responseSectionId = sectionResponse.sectionId
            require(expectedSectionId == responseSectionId) { throw InvalidSurveyResponseException() }
            val section = sections.first { it.id == responseSectionId }
            expectedSectionId = section.findNextSectionId(sectionResponse)
        }
        require(expectedSectionId is SectionId.End) { throw InvalidSurveyResponseException() }
    }

    fun updateContent(
        title: String,
        description: String,
        thumbnail: String?,
        finishMessage: String,
        rewardSetting: RewardSetting,
        isVisible: Boolean,
        isResultOpen: Boolean,
        sections: List<Section>,
    ): Survey {
        require(
            status == SurveyStatus.NOT_STARTED ||
                status == SurveyStatus.IN_MODIFICATION &&
                rewardSetting == this.rewardSetting,
        ) {
            throw InvalidUpdateSurveyException()
        }
        return fromComponents(
            id = this.id,
            title = title,
            description = description,
            thumbnail = thumbnail,
            publishedAt = this.publishedAt,
            status = this.status,
            finishMessage = finishMessage,
            rewardSetting = rewardSetting,
            isVisible = isVisible,
            makerId = this.makerId,
            isResultOpen = isResultOpen,
            sections = sections,
        )
    }

    fun finish() =
        fromComponents(
            id = id,
            title = title,
            description = description,
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            status = SurveyStatus.CLOSED,
            finishMessage = finishMessage,
            rewardSetting = rewardSetting,
            isVisible = isVisible,
            makerId = makerId,
            isResultOpen = isResultOpen,
            sections = sections,
        )

    fun start() =
        when (status) {
            SurveyStatus.NOT_STARTED ->
                fromComponents(
                    id = id,
                    title = title,
                    description = description,
                    thumbnail = thumbnail,
                    publishedAt = DateUtil.getCurrentDate(),
                    status = SurveyStatus.IN_PROGRESS,
                    finishMessage = finishMessage,
                    rewardSetting =
                        RewardSetting.of(
                            type = rewardSetting.type,
                            rewards = rewardSetting.rewards,
                            targetParticipantCount = rewardSetting.targetParticipantCount,
                            finishedAt = rewardSetting.finishedAt?.value,
                            surveyStatus = SurveyStatus.IN_PROGRESS,
                        ),
                    isVisible = isVisible,
                    makerId = makerId,
                    isResultOpen = isResultOpen,
                    sections = sections,
                )
            SurveyStatus.IN_MODIFICATION ->
                fromComponents(
                    id = id,
                    title = title,
                    description = description,
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    status = SurveyStatus.IN_PROGRESS,
                    finishMessage = finishMessage,
                    rewardSetting = rewardSetting,
                    isVisible = isVisible,
                    makerId = makerId,
                    isResultOpen = isResultOpen,
                    sections = sections,
                )
            SurveyStatus.IN_PROGRESS -> throw InvalidSurveyStartException()
            SurveyStatus.CLOSED -> throw InvalidSurveyStartException()
        }

    fun edit(): Survey {
        require(status == SurveyStatus.IN_PROGRESS) { throw InvalidSurveyEditException() }
        return fromComponents(
            id = id,
            title = title,
            description = description,
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            status = SurveyStatus.IN_MODIFICATION,
            finishMessage = finishMessage,
            rewardSetting = rewardSetting,
            isVisible = isVisible,
            makerId = makerId,
            isResultOpen = isResultOpen,
            sections = sections,
        )
    }

    fun isImmediateDraw() = rewardSetting.isImmediateDraw

    fun findSectionById(id: UUID): Section? = sections.find { it.id.value == id }

    fun findQuestionById(id: UUID): Question? =
        sections
            .flatMap { it.questions }
            .find { it.id == id }

    companion object {
        const val DEFAULT_TITLE = ""
        const val DEFAULT_DESCRIPTION = ""
        const val DEFAULT_FINISH_MESSAGE = ""

        fun create(makerId: UUID) =
            fromComponents(
                id = UUID.randomUUID(),
                title = DEFAULT_TITLE,
                description = DEFAULT_DESCRIPTION,
                thumbnail = null,
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                finishMessage = DEFAULT_FINISH_MESSAGE,
                rewardSetting = NoRewardSetting,
                isVisible = false,
                makerId = makerId,
                isResultOpen = false,
                sections = listOf(),
            )

        fun fromComponents(
            id: UUID,
            title: String,
            description: String,
            thumbnail: String?,
            publishedAt: Date?,
            status: SurveyStatus,
            finishMessage: String,
            rewardSetting: RewardSetting,
            isVisible: Boolean,
            makerId: UUID,
            isResultOpen: Boolean,
            sections: List<Section>,
        ): Survey {
            val entity =
                Survey(
                    id = id,
                    title = title,
                    description = description,
                    thumbnail = thumbnail,
                    publishedAt = publishedAt,
                    finishedAt = rewardSetting.finishedAt?.value,
                    status = status,
                    finishMessage = finishMessage,
                    targetParticipantCount = rewardSetting.targetParticipantCount,
                    rewardSettingType = rewardSetting.type,
                    isVisible = isVisible,
                    makerId = makerId,
                    isResultOpen = isResultOpen,
                )
            rewardSetting.rewards.forEachIndexed { index, reward ->
                entity.rewardEntities.add(RewardEntity.from(reward, entity, index))
            }
            sections.forEachIndexed { index, section ->
                entity.sectionEntities.add(SectionEntity.from(section, entity, index))
            }
            return entity
        }
    }
}
