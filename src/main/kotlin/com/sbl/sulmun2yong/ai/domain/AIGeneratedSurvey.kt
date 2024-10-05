package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import java.util.UUID

class AIGeneratedSurvey(
    val chatSessionId: UUID,
    val survey: Survey,
)
