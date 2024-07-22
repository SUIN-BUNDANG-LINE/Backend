package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Section
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Choices
import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.TextResponseQuestion
import com.sbl.sulmun2yong.survey.domain.routing.NumericalOrderRouting
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfig
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfigs
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteType
import com.sbl.sulmun2yong.survey.domain.routing.SetByChoiceRouting
import com.sbl.sulmun2yong.survey.domain.routing.SetByUserRouting
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

    fun toDomain() =
        Survey(
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
            sections = this.sections.map { it.toDomain() },
        )

    private fun RewardSubDocument.toDomain() =
        Reward(
            id = this.rewardId,
            name = this.name,
            category = this.category,
            count = this.count,
        )

    private fun SectionSubDocument.toDomain() =
        Section(
            id = this.sectionId,
            title = this.title,
            description = this.description,
            routeDetails = this.getRouteDetails(),
            questions = this.questions.map { it.toDomain() },
        )

    private fun SectionSubDocument.getRouteDetails() =
        when (this.routeType) {
            SectionRouteType.NUMERICAL_ORDER -> NumericalOrderRouting(this.nextSectionId)
            SectionRouteType.SET_BY_CHOICE ->
                SetByChoiceRouting(
                    this.keyQuestionId!!,
                    SectionRouteConfigs(
                        this.sectionRouteConfigs!!.map {
                            it.toDomain()
                        },
                    ),
                )
            SectionRouteType.SET_BY_USER -> SetByUserRouting(this.nextSectionId)
        }

    private fun SectionRouteConfigSubDocument.toDomain() =
        SectionRouteConfig(
            content = this.choiceContent,
            nextSectionId = this.nextSectionId,
        )

    private fun QuestionSubDocument.toDomain() =
        when (this.type) {
            QuestionType.SINGLE_CHOICE ->
                SingleChoiceQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    choices = Choices(this.choices!!),
                    isAllowOther = this.isAllowOther,
                )
            QuestionType.MULTIPLE_CHOICE ->
                MultipleChoiceQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                    choices = Choices(this.choices!!),
                    isAllowOther = this.isAllowOther,
                )
            QuestionType.TEXT_RESPONSE ->
                TextResponseQuestion(
                    id = this.questionId,
                    title = this.title,
                    description = this.description,
                    isRequired = this.isRequired,
                )
        }
}
