package com.sbl.sulmun2yong.ai.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse

data class AISurveyEditResponse(
    val editedSurvey: SurveyMakeInfoResponse,
    val originalData: SurveyData,
    val editedData: SurveyData,
) {
    companion object {
        fun of(
            editedSurvey: Survey,
            originalData: Survey,
            editedData: Survey,
        ): AISurveyEditResponse =
            AISurveyEditResponse(
                editedSurvey = SurveyMakeInfoResponse.of(editedSurvey),
                originalData = SurveyData.SurveyDataItem.of(originalData),
                editedData = SurveyData.SurveyDataItem.of(editedData),
            )

        fun of(
            editedSurvey: Survey,
            originalData: Section,
            editedData: Section,
        ): AISurveyEditResponse =
            AISurveyEditResponse(
                editedSurvey = SurveyMakeInfoResponse.of(editedSurvey),
                originalData = SurveyData.SectionDataItem.of(originalData),
                editedData = SurveyData.SectionDataItem.of(editedData),
            )

        fun of(
            editedSurvey: Survey,
            originalData: Question,
            editedData: Question,
        ): AISurveyEditResponse =
            AISurveyEditResponse(
                editedSurvey = SurveyMakeInfoResponse.of(editedSurvey),
                originalData = SurveyData.QuestionDataItem.of(originalData),
                editedData = SurveyData.QuestionDataItem.of(editedData),
            )
    }

    sealed class SurveyData {
        data class SurveyDataItem(
            val survey: SurveyMakeInfoResponse,
        ) : SurveyData() {
            companion object {
                fun of(survey: Survey): SurveyDataItem = SurveyDataItem(SurveyMakeInfoResponse.of(survey))
            }
        }

        data class SectionDataItem(
            val section: SurveyMakeInfoResponse.SectionMakeInfoResponse,
        ) : SurveyData() {
            companion object {
                fun of(section: Section): SectionDataItem = SectionDataItem(SurveyMakeInfoResponse.SectionMakeInfoResponse.from(section))
            }
        }

        data class QuestionDataItem(
            val question: SurveyMakeInfoResponse.QuestionMakeInfoResponse,
        ) : SurveyData() {
            companion object {
                fun of(question: Question): QuestionDataItem =
                    QuestionDataItem(SurveyMakeInfoResponse.QuestionMakeInfoResponse.from(question))
            }
        }
    }
}
