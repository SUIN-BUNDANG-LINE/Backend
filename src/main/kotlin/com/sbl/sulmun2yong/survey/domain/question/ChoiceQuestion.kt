package com.sbl.sulmun2yong.survey.domain.question

/** 선택지가 있는 질문 */
interface ChoiceQuestion : Question {
    override val choices: Choices

    // 응답이 전부 선택지에 포함된 경우만 유효
    override fun isValidResponse(questionResponse: QuestionResponse) = questionResponse.all { choices.isContains(it) }
}
