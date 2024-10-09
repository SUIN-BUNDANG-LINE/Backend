package com.sbl.sulmun2yong.ai.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import java.util.UUID

// TODO: 가능하면 도메인 로직으로 옮기기
data class AISurveyEditResponse(
    val surveyBasicInfo: SurveyInfoChangeDTO,
    val sections: List<SectionChangeDTO>,
) {
    data class SurveyInfoChangeDTO(
        val surveyId: UUID,
        val changeType: ChangeType,
        val originalData: SurveyBasicInfo?,
        val modifiedData: SurveyBasicInfo?,
    )

    data class SurveyBasicInfo(
        val title: String,
        val description: String,
        val finishMessage: String,
    )

    data class SectionChangeDTO(
        val sectionBasicInfo: SectionInfoChangeDTO,
        val questions: List<QuestionChangeDTO>,
    )

    data class SectionInfoChangeDTO(
        val sectionId: UUID,
        val changeType: ChangeType,
        val originalData: SectionBasicInfo?,
        val modifiedData: SectionBasicInfo?,
    )

    data class SectionBasicInfo(
        val title: String,
        val description: String,
    )

    data class QuestionChangeDTO(
        val questionId: UUID,
        val changeType: ChangeType,
        val originalData: QuestionBasicInfo?,
        val modifiedData: QuestionBasicInfo?,
    )

    data class QuestionBasicInfo(
        val type: QuestionType,
        val title: String,
        val description: String,
        val isRequired: Boolean,
        val isAllowOther: Boolean,
        val choices: List<String>?,
    )

    enum class ChangeType {
        UNCHANGED,
        MODIFIED,
        DELETED,
        CREATED,
    }

    companion object {
        fun compareSurveys(
            originalSurvey: Survey,
            editedSurvey: Survey,
        ): AISurveyEditResponse {
            val originalSurveyMakeInfoResponse = SurveyMakeInfoResponse.of(originalSurvey)
            val editedSurveyMakeInfoResponse = SurveyMakeInfoResponse.of(editedSurvey)

            val surveyInfoChange =
                compareSurveyInfo(
                    originalSurvey.id,
                    originalSurveyMakeInfoResponse.title,
                    originalSurveyMakeInfoResponse.description,
                    originalSurveyMakeInfoResponse.finishMessage,
                    editedSurveyMakeInfoResponse.title,
                    editedSurveyMakeInfoResponse.description,
                    editedSurveyMakeInfoResponse.finishMessage,
                )

            val sectionChanges =
                originalSurveyMakeInfoResponse.sections.map { originalSection ->
                    val matchingModifiedSection =
                        editedSurveyMakeInfoResponse.sections.find { it.sectionId == originalSection.sectionId }
                    compareSections(originalSection, matchingModifiedSection)
                } +
                    editedSurveyMakeInfoResponse.sections
                        .filter { newSection ->
                            originalSurveyMakeInfoResponse.sections.none { it.sectionId == newSection.sectionId }
                        }.map { newSection ->
                            SectionChangeDTO(
                                SectionInfoChangeDTO(
                                    newSection.sectionId,
                                    ChangeType.CREATED,
                                    null,
                                    SectionBasicInfo(newSection.title, newSection.description),
                                ),
                                newSection.questions.map { newQuestion ->
                                    QuestionChangeDTO(
                                        questionId = newQuestion.questionId,
                                        ChangeType.CREATED,
                                        null,
                                        QuestionBasicInfo(
                                            newQuestion.type,
                                            newQuestion.title,
                                            newQuestion.description,
                                            newQuestion.isRequired,
                                            newQuestion.isAllowOther,
                                            newQuestion.choices,
                                        ),
                                    )
                                },
                            )
                        }

            return AISurveyEditResponse(surveyInfoChange, sectionChanges)
        }

        private fun compareSurveyInfo(
            surveyId: UUID,
            originalTitle: String,
            originalDescription: String,
            originalFinishMessage: String,
            modifiedTitle: String,
            modifiedDescription: String,
            modifiedFinishMessage: String,
        ): SurveyInfoChangeDTO {
            val originalInfo = SurveyBasicInfo(originalTitle, originalDescription, originalFinishMessage)
            val modifiedInfo = SurveyBasicInfo(modifiedTitle, modifiedDescription, modifiedFinishMessage)

            val changeType = if (originalInfo == modifiedInfo) ChangeType.UNCHANGED else ChangeType.MODIFIED

            return SurveyInfoChangeDTO(surveyId, changeType, originalInfo, modifiedInfo)
        }

        private fun compareSections(
            originalSection: SurveyMakeInfoResponse.SectionMakeInfoResponse,
            modifiedSection: SurveyMakeInfoResponse.SectionMakeInfoResponse?,
        ): SectionChangeDTO {
            val changeType =
                when {
                    modifiedSection == null -> ChangeType.DELETED
                    originalSection.title == modifiedSection.title && originalSection.description == modifiedSection.description
                    -> ChangeType.UNCHANGED
                    else -> ChangeType.MODIFIED
                }

            val originalInfo = SectionBasicInfo(originalSection.title, originalSection.description)
            val modifiedInfo = modifiedSection?.let { SectionBasicInfo(it.title, it.description) }

            val questionChanges =
                originalSection.questions.map { originalQuestion ->
                    val matchingModifiedQuestion =
                        modifiedSection?.questions?.find { it.questionId == originalQuestion.questionId }
                    compareQuestions(originalQuestion, matchingModifiedQuestion)
                } + (
                    modifiedSection
                        ?.questions
                        ?.filter { newQuestion ->
                            originalSection.questions.none { it.questionId == newQuestion.questionId }
                        }?.map { newQuestion ->
                            QuestionChangeDTO(
                                newQuestion.questionId,
                                ChangeType.CREATED,
                                null,
                                QuestionBasicInfo(
                                    newQuestion.type,
                                    newQuestion.title,
                                    newQuestion.description,
                                    newQuestion.isRequired,
                                    newQuestion.isAllowOther,
                                    newQuestion.choices,
                                ),
                            )
                        }
                        ?: emptyList()
                )

            return SectionChangeDTO(
                SectionInfoChangeDTO(originalSection.sectionId, changeType, originalInfo, modifiedInfo),
                questionChanges,
            )
        }

        private fun compareQuestions(
            originalQuestion: SurveyMakeInfoResponse.QuestionMakeInfoResponse,
            modifiedQuestion: SurveyMakeInfoResponse.QuestionMakeInfoResponse?,
        ): QuestionChangeDTO {
            val changeType =
                when {
                    modifiedQuestion == null -> ChangeType.DELETED
                    originalQuestion == modifiedQuestion -> ChangeType.UNCHANGED
                    else -> ChangeType.MODIFIED
                }

            val originalInfo =
                QuestionBasicInfo(
                    originalQuestion.type,
                    originalQuestion.title,
                    originalQuestion.description,
                    originalQuestion.isRequired,
                    originalQuestion.isAllowOther,
                    originalQuestion.choices,
                )
            val modifiedInfo =
                modifiedQuestion?.let {
                    QuestionBasicInfo(it.type, it.title, it.description, it.isRequired, it.isAllowOther, it.choices)
                }

            return QuestionChangeDTO(originalQuestion.questionId, changeType, originalInfo, modifiedInfo)
        }
    }
}
