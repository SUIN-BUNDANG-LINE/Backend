package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import java.util.UUID

class AIEditLog(
    val id: UUID,
    val surveyId: UUID,
    val makerId: UUID,
    val userPrompt: String,
    val originalSurvey: Survey,
    val editedSurvey: Survey,
)
