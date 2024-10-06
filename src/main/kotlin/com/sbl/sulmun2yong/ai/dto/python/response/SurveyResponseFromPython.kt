package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey

data class SurveyResponseFromPython(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<SectionResponseFromPython>,
) {
    fun toDomain(): PythonFormattedSurvey =
        PythonFormattedSurvey(
            title = title,
            description = description,
            finishMessage = finishMessage,
            sections = sections.map { it.toDomain() },
        )
}
