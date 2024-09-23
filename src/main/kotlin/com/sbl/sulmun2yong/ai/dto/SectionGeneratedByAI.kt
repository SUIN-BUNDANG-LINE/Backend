package com.sbl.sulmun2yong.ai.dto

import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import java.util.UUID

class SectionGeneratedByAI(
    val title: String,
    val description: String,
    val questions: List<QuestionGeneratedByAI>,
) {
    fun toDomain(): Section =
        Section(
            id = SectionId.Standard(UUID.randomUUID()),
            title = title,
            description = description,
            routingStrategy = null,
            questions = questions.map { it.toDomain() },
            sectionIds = null,
        )
}
