package com.sbl.sulmun2yong.survey.dto

import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.routing.RoutingType
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.Date
import java.util.UUID

data class SurveyCreateRequest(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String,
    val publishedAt: Date,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int,
    val rewards: List<RewardCreateRequest>,
    val sections: List<SectionCreateRequest>,
) {
    data class RewardCreateRequest(
        val name: String,
        val category: String,
        val count: Int,
    ) {
        fun toSurveyDomain() = Reward(id = UUID.randomUUID(), name = name, category = category, count = count)

        fun toRewardDomain() =
            com.sbl.sulmun2yong.drawing.domain
                .Reward(name = name, category = category, count = count)
    }

    data class SectionCreateRequest(
        val id: UUID,
        val title: String,
        val description: String,
        val questions: List<QuestionCreateRequest>,
        val routingType: RoutingType,
        val nextSectionId: UUID?,
        val keyQuestionId: UUID?,
        val routingConfigs: List<RoutingConfigCreateRequest>?,
    ) {
        fun toDomain(sectionIds: SectionIds) =
            Section(
                id = SectionId.Standard(id),
                title = title,
                description = description,
                routingStrategy =
                    when (routingType) {
                        RoutingType.NUMERICAL_ORDER -> RoutingStrategy.NumericalOrder
                        RoutingType.SET_BY_USER -> RoutingStrategy.SetByUser(SectionId.from(nextSectionId))
                        RoutingType.SET_BY_CHOICE ->
                            RoutingStrategy.SetByChoice(
                                keyQuestionId = keyQuestionId!!,
                                routingMap = routingConfigs!!.map { Choice.from(it.content) to SectionId.from(it.nextSectionId) }.toMap(),
                            )
                    },
                questions = questions.map { it.toDomain() },
                sectionIds = sectionIds,
            )
    }

    data class RoutingConfigCreateRequest(
        val content: String?,
        val nextSectionId: UUID?,
    )

    data class QuestionCreateRequest(
        val id: UUID,
        val type: QuestionType,
        val title: String,
        val description: String,
        val isRequired: Boolean,
        val isAllowOther: Boolean,
        val choices: List<String>?,
    ) {
        fun toDomain() =
            when (type) {
                QuestionType.TEXT_RESPONSE ->
                    StandardTextQuestion(
                        id = id,
                        title = title,
                        description = description,
                        isRequired = isRequired,
                    )
                QuestionType.SINGLE_CHOICE ->
                    StandardSingleChoiceQuestion(
                        id = id,
                        title = title,
                        description = description,
                        isRequired = isRequired,
                        choices =
                            Choices(
                                standardChoices = choices!!.map { Choice.Standard(it) },
                                isAllowOther = isAllowOther,
                            ),
                    )
                QuestionType.MULTIPLE_CHOICE ->
                    StandardMultipleChoiceQuestion(
                        id = id,
                        title = title,
                        description = description,
                        isRequired = isRequired,
                        choices =
                            Choices(
                                standardChoices = choices!!.map { Choice.Standard(it) },
                                isAllowOther = isAllowOther,
                            ),
                    )
            }
    }
}
