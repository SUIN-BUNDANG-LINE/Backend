package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.routing.RoutingType
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.util.UUID

@Document(collection = "surveys")
data class SurveyDocument(
    @Id
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String?,
    val publishedAt: Date?,
    val finishedAt: Date?,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int?,
    val rewardSettingType: RewardSettingType,
    val isVisible: Boolean,
    val makerId: UUID,
    val isResultOpen: Boolean,
    val rewards: List<RewardSubDocument>,
    val sections: List<SectionSubDocument>,
    val isDeleted: Boolean = false,
) : BaseTimeDocument() {
    companion object {
        fun from(survey: Survey) =
            SurveyDocument(
                id = survey.id,
                title = survey.title,
                description = survey.description,
                thumbnail = survey.thumbnail,
                publishedAt = survey.publishedAt,
                finishedAt = survey.rewardSetting.finishedAt?.value,
                status = survey.status,
                finishMessage = survey.finishMessage,
                targetParticipantCount = survey.rewardSetting.targetParticipantCount,
                makerId = survey.makerId,
                isResultOpen = survey.isResultOpen,
                rewards = survey.rewardSetting.rewards.map { it.toDocument() },
                rewardSettingType = survey.rewardSetting.type,
                isVisible = survey.isVisible,
                sections = survey.sections.map { it.toDocument() },
            )

        private fun Reward.toDocument() =
            RewardSubDocument(
                name = this.name,
                category = this.category,
                count = this.count,
            )

        private fun Section.toDocument() =
            SectionSubDocument(
                sectionId = this.id.value,
                title = this.title,
                description = this.description,
                routeType = this.routingStrategy.type,
                nextSectionId =
                    when (this.routingStrategy) {
                        is RoutingStrategy.SetByUser -> this.routingStrategy.nextSectionId.value
                        else -> null
                    },
                keyQuestionId =
                    when (this.routingStrategy) {
                        is RoutingStrategy.SetByChoice -> this.routingStrategy.keyQuestionId
                        else -> null
                    },
                sectionRouteConfigs =
                    when (this.routingStrategy) {
                        is RoutingStrategy.SetByChoice ->
                            this.routingStrategy.routingMap.map { (choice, sectionId) ->
                                SectionRouteConfigSubDocument(
                                    choiceContent = choice.content,
                                    nextSectionId = sectionId.value,
                                )
                            }
                        else -> null
                    },
                questions = this.questions.map { it.toDocument() },
            )

        private fun Question.toDocument() =
            QuestionSubDocument(
                questionId = this.id,
                title = this.title,
                description = this.description,
                isRequired = this.isRequired,
                type = this.questionType,
                choices =
                    when (this) {
                        is StandardSingleChoiceQuestion -> this.choices.standardChoices.map { it.content }
                        is StandardMultipleChoiceQuestion -> this.choices.standardChoices.map { it.content }
                        else -> null
                    },
                isAllowOther =
                    when (this) {
                        is StandardSingleChoiceQuestion -> this.choices.isAllowOther
                        is StandardMultipleChoiceQuestion -> this.choices.isAllowOther
                        else -> false
                    },
            )
    }

    data class RewardSubDocument(
        val name: String,
        val category: String,
        val count: Int,
    )

    data class SectionSubDocument(
        val sectionId: UUID,
        val title: String,
        val description: String,
        val routeType: RoutingType,
        val nextSectionId: UUID?,
        val keyQuestionId: UUID?,
        val sectionRouteConfigs: List<SectionRouteConfigSubDocument>?,
        val questions: List<QuestionSubDocument>,
    )

    data class SectionRouteConfigSubDocument(
        val choiceContent: String?,
        val nextSectionId: UUID?,
    )

    data class QuestionSubDocument(
        val questionId: UUID,
        val title: String,
        val description: String,
        val isRequired: Boolean,
        val type: QuestionType,
        val choices: List<String>?,
        val isAllowOther: Boolean,
    )

    fun toDomain(): Survey =
        Survey(
            id = this.id,
            title = this.title,
            description = this.description,
            thumbnail = this.thumbnail,
            publishedAt = this.publishedAt,
            status = this.status,
            finishMessage = this.finishMessage,
            rewardSetting =
                RewardSetting.of(
                    this.rewardSettingType,
                    this.rewards.map { it.toDomain() },
                    this.targetParticipantCount,
                    this.finishedAt,
                    this.status,
                ),
            isVisible = this.isVisible,
            isResultOpen = this.isResultOpen,
            makerId = this.makerId,
            sections = this.sections.toDomain(),
        )

    private fun List<SectionSubDocument>.toDomain() =
        if (this.isEmpty()) {
            listOf()
        } else {
            val sectionIds = SectionIds.from(this.map { SectionId.Standard(it.sectionId) })
            this.map { it.toDomain(sectionIds) }
        }

    private fun RewardSubDocument.toDomain() =
        Reward(
            name = this.name,
            category = this.category,
            count = this.count,
        )

    private fun SectionSubDocument.toDomain(sectionIds: SectionIds) =
        Section(
            id = SectionId.Standard(this.sectionId),
            title = this.title,
            description = this.description,
            routingStrategy = this.getRouteDetails(),
            questions = this.questions.map { it.toDomain() },
            sectionIds = sectionIds,
        )

    // TODO: 직접 RouteStrategy 클래스를 넣도록 수정
    private fun SectionSubDocument.getRouteDetails() =
        when (this.routeType) {
            RoutingType.NUMERICAL_ORDER -> RoutingStrategy.NumericalOrder
            RoutingType.SET_BY_CHOICE ->
                RoutingStrategy.SetByChoice(
                    keyQuestionId = this.keyQuestionId!!,
                    routingMap = this.sectionRouteConfigs?.toDomain() ?: emptyMap(),
                )
            RoutingType.SET_BY_USER -> RoutingStrategy.SetByUser(SectionId.from(this.nextSectionId))
        }

    private fun List<SectionRouteConfigSubDocument>.toDomain(): Map<Choice, SectionId> =
        this.associate {
            Choice.from(it.choiceContent) to SectionId.from(it.nextSectionId)
        }

    private fun QuestionSubDocument.toDomain() =
        when (this.type) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    // TODO: Document를 Domain클래스로 변환 중에 생긴 에러는 여기서 직접 반환하도록 수정
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    choices = Choices(this.choices?.map { Choice.Standard(it) } ?: listOf(), isAllowOther),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                )
        }
}
