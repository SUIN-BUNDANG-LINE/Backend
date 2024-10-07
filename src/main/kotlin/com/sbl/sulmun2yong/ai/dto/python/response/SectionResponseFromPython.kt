package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection

data class SectionResponseFromPython(
    val title: String,
    val description: String,
    val questions: List<QuestionResponseFromPython>,
) {
    fun toDomain() =
        PythonFormattedSection(
            title = title,
            description = description,
            questions = questions.map { it.toDomain() },
        )
}
