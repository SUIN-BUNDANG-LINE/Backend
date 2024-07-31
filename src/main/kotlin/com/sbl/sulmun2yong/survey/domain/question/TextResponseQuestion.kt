package com.sbl.sulmun2yong.survey.domain.question

/** 주관식 질문 */
interface TextResponseQuestion : Question {
    override fun isValidResponse(questionResponse: QuestionResponse) = questionResponse.size == 1 && !questionResponse.first().isOther
}
