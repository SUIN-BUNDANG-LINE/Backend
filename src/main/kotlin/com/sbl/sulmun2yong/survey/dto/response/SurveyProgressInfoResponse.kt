package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
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
                sections =
                    survey.sections.map { section ->
                        SectionInfo(
                            sectionId = section.id,
                            title = section.title,
                            description = section.description,
                            routeDetails =
                                RouteDetailsInfo(
                                    type = section.routeDetails.type,
                                    nextSectionId = section.routeDetails.nextSectionId,
                                    keyQuestionId = section.routeDetails.keyQuestionId,
                                    sectionRouteConfigs =
                                        section.routeDetails.sectionRouteConfigs?.map { config ->
                                            SectionRouteConfigInfo(
                                                content = config.content,
                                                nextSectionId = config.nextSectionId,
                                            )
                                        },
                                ),
                            questions =
                                section.questions.map { question ->
                                    QuestionInfo(
                                        questionId = question.id,
                                        title = question.title,
                                        description = question.description,
                                        isRequired = question.isRequired,
                                        type = question.questionType,
                                        choices = question.choices,
                                        isAllowOther = question.isAllowOther,
                                    )
                                },
                        )
                    },
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
