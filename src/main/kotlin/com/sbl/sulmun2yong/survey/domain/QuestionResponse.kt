package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseCommand
import java.util.UUID

data class QuestionResponse(
    val questionId: UUID,
    val responses: ResponseCommand,
)
