package com.sbl.sulmun2yong.survey.dto.request

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
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
import java.util.Date
import java.util.UUID

data class SurveySaveRequest(
    val title: String,
    val description: String,
    // TODO: 섬네일의 URL이 우리 서비스의 S3 URL인지 확인하기
    val thumbnail: String?,
    val finishMessage: String,
    val isVisible: Boolean,
    val rewardSetting: RewardSettingResponse,
    val sections: List<SectionCreateRequest>,
) {
    fun List<SectionCreateRequest>.toDomain() =
        if (isEmpty()) {
            listOf()
        } else {
            val sectionIds = SectionIds.from(this.map { SectionId.Standard(it.sectionId) })
            this.map {
                Section(
                    id = SectionId.Standard(it.sectionId),
                    title = it.title,
                    description = it.description,
                    routingStrategy = it.getRoutingStrategy(),
                    questions = it.questions.map { question -> question.toDomain() },
                    sectionIds = sectionIds,
                )
            }
        }

    data class RewardSettingResponse(
        val type: RewardSettingType,
        val rewards: List<RewardCreateRequest>,
        val targetParticipantCount: Int?,
        val finishedAt: Date?,
    ) {
        fun toDomain(surveyStatus: SurveyStatus) =
            RewardSetting.of(
                type,
                rewards.map { Reward(it.name, it.category, it.count) },
                targetParticipantCount,
                finishedAt,
                surveyStatus,
            )
    }

    data class RewardCreateRequest(
        val name: String,
        val category: String,
        val count: Int,
    )

    data class SectionCreateRequest(
        val sectionId: UUID,
        val title: String,
        val description: String,
        val questions: List<QuestionCreateRequest>,
        val routeDetails: RouteDetailsCreateRequest,
    ) {
        fun getRoutingStrategy() =
            when (routeDetails.type) {
                RoutingType.NUMERICAL_ORDER -> RoutingStrategy.NumericalOrder
                RoutingType.SET_BY_USER -> RoutingStrategy.SetByUser(SectionId.from(routeDetails.nextSectionId))
                RoutingType.SET_BY_CHOICE ->
                    RoutingStrategy.SetByChoice(
                        keyQuestionId = routeDetails.keyQuestionId!!,
                        routingMap =
                            routeDetails.sectionRouteConfigs!!.associate {
                                Choice.from(it.content) to SectionId.from(it.nextSectionId)
                            },
                    )
            }
    }

    data class RouteDetailsCreateRequest(
        val type: RoutingType,
        val nextSectionId: UUID?,
        val keyQuestionId: UUID?,
        val sectionRouteConfigs: List<RoutingConfigCreateRequest>?,
    )

    data class RoutingConfigCreateRequest(
        val content: String?,
        val nextSectionId: UUID?,
    )

    data class QuestionCreateRequest(
        val questionId: UUID,
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
                        id = questionId,
                        title = title,
                        description = description,
                        isRequired = isRequired,
                    )
                QuestionType.SINGLE_CHOICE ->
                    StandardSingleChoiceQuestion(
                        id = questionId,
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
                        id = questionId,
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
