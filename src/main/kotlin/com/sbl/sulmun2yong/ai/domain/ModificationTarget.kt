package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section

sealed class ModificationTarget {
    data class SurveyTarget(
        val survey: Survey,
    ) : ModificationTarget()

    data class SectionTarget(
        val section: Section,
    ) : ModificationTarget()

    data class QuestionTarget(
        val question: Question,
    ) : ModificationTarget()
}
