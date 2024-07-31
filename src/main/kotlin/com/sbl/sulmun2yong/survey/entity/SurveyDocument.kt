package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.question.Choices
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.StandardTextResponseQuestion
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteType
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
    val thumbnail: String,
    val publishedAt: Date?,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int,
    val rewards: List<RewardSubDocument>,
    val sections: List<SectionSubDocument>,
) : BaseTimeDocument() {
    data class RewardSubDocument(
        val rewardId: UUID,
        val name: String,
        val category: String,
        val count: Int,
    )

    data class SectionSubDocument(
        val sectionId: UUID,
        val title: String,
        val description: String,
        val routeType: SectionRouteType,
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

    fun toDomain(): Survey {
        val sectionIds = SectionIds(this.sections.map { SectionId.Standard(it.sectionId) })
        return Survey(
            id = this.id,
            title = this.title,
            description = this.description,
            thumbnail = this.thumbnail,
            finishedAt = this.finishedAt,
            publishedAt = this.publishedAt,
            status = this.status,
            finishMessage = this.finishMessage,
            targetParticipantCount = this.targetParticipantCount,
            rewards = this.rewards.map { it.toDomain() },
            sections = this.sections.map { it.toDomain(sectionIds) },
        )
    }

    private fun RewardSubDocument.toDomain() =
        Reward(
            id = this.rewardId,
            name = this.name,
            category = this.category,
            count = this.count,
        )

    private fun SectionSubDocument.toDomain(sectionIds: SectionIds) =
        Section(
            id = SectionId.Standard(this.sectionId),
            title = this.title,
            description = this.description,
            routeDetails = this.getRouteDetails(),
            questions = this.questions.map { it.toDomain() },
            sectionIds = sectionIds,
        )

    private fun SectionSubDocument.getRouteDetails() =
        when (this.routeType) {
            SectionRouteType.NUMERICAL_ORDER -> RouteDetails.NumericalOrderRouting
            SectionRouteType.SET_BY_CHOICE ->
                RouteDetails.SetByChoiceRouting(
                    keyQuestionId = this.keyQuestionId!!,
                    sectionRouteConfigs = this.sectionRouteConfigs?.toDomain() ?: emptyMap(),
                )
            SectionRouteType.SET_BY_USER -> RouteDetails.SetByUserRouting(SectionId.from(this.nextSectionId))
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
                StandardTextResponseQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                )
        }
}
