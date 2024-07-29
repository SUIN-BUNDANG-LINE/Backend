package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.NextSectionId
import com.sbl.sulmun2yong.survey.domain.Section
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteType
import java.util.UUID

data class SurveyProgressInfoResponse(
    val title: String,
    val finishMessage: String,
    val sections: List<SectionInfo>,
) {
    companion object {
        fun of(survey: Survey) =
            SurveyProgressInfoResponse(
                title = survey.title,
                finishMessage = survey.finishMessage,
                sections = survey.sections.map { it.toDto() },
            )

        private fun Section.toDto() =
            SectionInfo(
                sectionId = this.id,
                title = this.title,
                description = this.description,
                routeDetails = this.routeDetails.toDto(),
                questions = this.questions.map { it.toDto() },
            )

        private fun RouteDetails.toDto() =
            RouteDetailsInfo(
                type = this.type,
                nextSectionId = if (this is RouteDetails.SetByUserRouting) this.nextSectionId.value else null,
                keyQuestionId = if (this is RouteDetails.SetByChoiceRouting) this.keyQuestionId else null,
                sectionRouteConfigs = if (this is RouteDetails.SetByChoiceRouting) this.sectionRouteConfigs.toDto() else null,
            )

        private fun Map<Choice, NextSectionId>.toDto() =
            this.map { (choice, nextSectionId) ->
                SectionRouteConfigInfo(
                    content = choice.content,
                    nextSectionId = nextSectionId.value,
                )
            }

        private fun Question.toDto() =
            QuestionInfo(
                questionId = this.id,
                title = this.title,
                description = this.description,
                isRequired = this.isRequired,
                type = this.questionType,
                choices = this.choices?.standardChoices?.map { it.content },
                isAllowOther = this.choices?.isAllowOther ?: false,
            )
    }

    data class SectionInfo(
        val sectionId: UUID,
        val title: String,
        val description: String,
        val routeDetails: RouteDetailsInfo,
        val questions: List<QuestionInfo>,
    )

    data class RouteDetailsInfo(
        val type: SectionRouteType,
        val nextSectionId: UUID?,
        val keyQuestionId: UUID?,
        val sectionRouteConfigs: List<SectionRouteConfigInfo>?,
    )

    data class SectionRouteConfigInfo(
        val content: String?,
        val nextSectionId: UUID?,
    )

    data class QuestionInfo(
        val questionId: UUID,
        val title: String,
        val description: String,
        val isRequired: Boolean,
        val type: QuestionType,
        val choices: List<String>?,
        val isAllowOther: Boolean,
    )
}
