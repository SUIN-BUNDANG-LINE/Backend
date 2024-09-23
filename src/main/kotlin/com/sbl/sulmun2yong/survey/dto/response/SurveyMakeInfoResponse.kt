package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.ai.dto.QuestionGeneratedByAI
import com.sbl.sulmun2yong.ai.dto.SectionGeneratedByAI
import com.sbl.sulmun2yong.ai.dto.SurveyGeneratedByAI
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.reward.RewardSettingType
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.routing.RoutingType
import com.sbl.sulmun2yong.survey.domain.section.Section
import java.util.Date
import java.util.UUID

data class SurveyMakeInfoResponse(
    val title: String,
    val description: String,
    val thumbnail: String?,
    val publishedAt: Date?,
    val rewardSetting: RewardSettingResponse,
    val status: SurveyStatus,
    val finishMessage: String,
    val isVisible: Boolean,
    val sections: List<SectionMakeInfoResponse>,
) {
    companion object {
        fun of(survey: Survey) =
            SurveyMakeInfoResponse(
                title = survey.title,
                description = survey.description,
                thumbnail = survey.thumbnail,
                publishedAt = survey.publishedAt,
                rewardSetting =
                    RewardSettingResponse(
                        type = survey.rewardSetting.type,
                        rewards = survey.rewardSetting.rewards.map { RewardMakeInfoResponse(it.name, it.category, it.count) },
                        targetParticipantCount = survey.rewardSetting.targetParticipantCount,
                        finishedAt = survey.rewardSetting.finishedAt?.value,
                    ),
                status = survey.status,
                finishMessage = survey.finishMessage,
                isVisible = survey.isVisible,
                sections = survey.sections.map { SectionMakeInfoResponse.from(it) },
            )

        fun of(surveyGeneratedByAI: SurveyGeneratedByAI) =
            SurveyMakeInfoResponse(
                title = surveyGeneratedByAI.title,
                description = surveyGeneratedByAI.description,
                thumbnail = null,
                publishedAt = null,
                rewardSetting =
                    RewardSettingResponse(
                        type = RewardSettingType.NO_REWARD,
                        rewards = emptyList(),
                        targetParticipantCount = null,
                        finishedAt = null,
                    ),
                status = SurveyStatus.NOT_STARTED,
                finishMessage = "",
                isVisible = true,
                sections = surveyGeneratedByAI.sections.map { SectionMakeInfoResponse.from(it) },
            )
    }

    data class RewardSettingResponse(
        val type: RewardSettingType,
        val rewards: List<RewardMakeInfoResponse>,
        val targetParticipantCount: Int?,
        val finishedAt: Date?,
    )

    data class RewardMakeInfoResponse(
        val name: String,
        val category: String,
        val count: Int,
    )

    data class SectionMakeInfoResponse(
        val sectionId: UUID,
        val title: String,
        val description: String,
        val questions: List<QuestionMakeInfoResponse>,
        val routeDetails: RouteDetailsMakeInfoResponse,
    ) {
        companion object {
            fun from(section: Section) =
                SectionMakeInfoResponse(
                    sectionId = section.id.value,
                    title = section.title,
                    description = section.description,
                    questions = section.questions.map { QuestionMakeInfoResponse.from(it) },
                    routeDetails = RouteDetailsMakeInfoResponse.from(section.routingStrategy),
                )

            fun from(sectionGeneratedByAI: SectionGeneratedByAI) =
                SectionMakeInfoResponse(
                    sectionId = UUID.randomUUID(),
                    title = sectionGeneratedByAI.title,
                    description = sectionGeneratedByAI.description,
                    questions = sectionGeneratedByAI.questions.map { QuestionMakeInfoResponse.of(it) },
                    routeDetails = RouteDetailsMakeInfoResponse.from(RoutingStrategy.NumericalOrder),
                )
        }
    }

    data class RouteDetailsMakeInfoResponse(
        val type: RoutingType,
        val nextSectionId: UUID?,
        val keyQuestionId: UUID?,
        val sectionRouteConfigs: List<RoutingConfigMakeInfoResponse>?,
    ) {
        companion object {
            fun from(routingStrategy: RoutingStrategy) =
                RouteDetailsMakeInfoResponse(
                    type = routingStrategy.type,
                    nextSectionId = if (routingStrategy is RoutingStrategy.SetByUser) routingStrategy.nextSectionId.value else null,
                    keyQuestionId = if (routingStrategy is RoutingStrategy.SetByChoice) routingStrategy.keyQuestionId else null,
                    sectionRouteConfigs =
                        if (routingStrategy is RoutingStrategy.SetByChoice) {
                            routingStrategy.routingMap.map { (choice, nextSectionId) ->
                                RoutingConfigMakeInfoResponse(
                                    content = choice.content,
                                    nextSectionId = nextSectionId.value,
                                )
                            }
                        } else {
                            null
                        },
                )
        }
    }

    data class RoutingConfigMakeInfoResponse(
        val content: String?,
        val nextSectionId: UUID?,
    )

    data class QuestionMakeInfoResponse(
        val questionId: UUID,
        val type: QuestionType,
        val title: String,
        val description: String,
        val isRequired: Boolean,
        val isAllowOther: Boolean,
        val choices: List<String>?,
    ) {
        companion object {
            fun from(question: Question) =
                QuestionMakeInfoResponse(
                    questionId = question.id,
                    type = question.questionType,
                    title = question.title,
                    description = question.description,
                    isRequired = question.isRequired,
                    isAllowOther = question.choices?.isAllowOther ?: false,
                    choices = question.choices?.standardChoices?.map { it.content },
                )

            fun of(questionGeneratedByAI: QuestionGeneratedByAI) =
                QuestionMakeInfoResponse(
                    questionId = UUID.randomUUID(),
                    type = questionGeneratedByAI.questionType,
                    title = questionGeneratedByAI.title,
                    description = "",
                    isRequired = questionGeneratedByAI.isRequired,
                    isAllowOther = questionGeneratedByAI.isAllowOther,
                    choices = questionGeneratedByAI.choices.ifEmpty { null },
                )
        }
    }
}
