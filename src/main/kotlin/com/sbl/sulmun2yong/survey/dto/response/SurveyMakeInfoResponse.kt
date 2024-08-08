package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
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
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int,
    val rewards: List<RewardMakeInfoResponse>,
    val sections: List<SectionMakeInfoResponse>,
) {
    companion object {
        fun of(survey: Survey) =
            SurveyMakeInfoResponse(
                title = survey.title,
                description = survey.description,
                thumbnail = survey.thumbnail,
                publishedAt = survey.publishedAt,
                finishedAt = survey.finishedAt,
                status = survey.status,
                finishMessage = survey.finishMessage,
                targetParticipantCount = survey.targetParticipantCount,
                rewards = survey.rewards.map { RewardMakeInfoResponse(it.name, it.category, it.count) },
                sections = survey.sections.map { SectionMakeInfoResponse.from(it) },
            )
    }

    data class RewardMakeInfoResponse(
        val name: String,
        val category: String,
        val count: Int,
    )

    data class SectionMakeInfoResponse(
        val id: UUID,
        val title: String,
        val description: String,
        val questions: List<QuestionMakeInfoResponse>,
        val routeDetails: RouteDetailsMakeInfoResponse,
    ) {
        companion object {
            fun from(section: Section) =
                SectionMakeInfoResponse(
                    id = section.id.value,
                    title = section.title,
                    description = section.description,
                    questions = section.questions.map { QuestionMakeInfoResponse.from(it) },
                    routeDetails = RouteDetailsMakeInfoResponse.from(section.routingStrategy),
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
        val id: UUID,
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
                    id = question.id,
                    type = question.questionType,
                    title = question.title,
                    description = question.description,
                    isRequired = question.isRequired,
                    isAllowOther = question.choices?.isAllowOther ?: false,
                    choices = question.choices?.standardChoices?.map { it.content },
                )
        }
    }
}
