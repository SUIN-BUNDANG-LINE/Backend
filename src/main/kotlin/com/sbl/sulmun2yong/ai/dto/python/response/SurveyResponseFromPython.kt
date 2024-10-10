package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import java.util.UUID

data class SurveyResponseFromPython(
    val id: UUID?,
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<SectionResponseFromPython>,
) {
    fun toDomain(): PythonFormattedSurvey =
        PythonFormattedSurvey(
            id = id,
            title = title,
            description = description,
            finishMessage = finishMessage,
            sections = sections.map { it.toDomain() },
        )
}
