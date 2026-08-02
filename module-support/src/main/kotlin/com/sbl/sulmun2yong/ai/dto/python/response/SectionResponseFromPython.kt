package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection
import java.util.UUID

data class SectionResponseFromPython(
    val id: UUID?,
    val title: String,
    val description: String,
    val questions: List<QuestionResponseFromPython>,
) {
    fun toDomain() =
        PythonFormattedSection(
            id = id,
            title = title,
            description = description,
            questions = questions.map { it.toDomain() },
        )
}
