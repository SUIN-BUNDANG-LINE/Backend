package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse

/** 주관식 질문 */
interface TextQuestion : Question {
    override fun isValidResponse(questionResponse: QuestionResponse) = questionResponse.size == 1 && !questionResponse.first().isOther

    override fun canBeKeyQuestion() = false
}
