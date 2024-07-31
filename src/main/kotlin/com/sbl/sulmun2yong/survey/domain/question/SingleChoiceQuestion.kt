package com.sbl.sulmun2yong.survey.domain.question

/** 단일 선택지 질문, keyQuestion이 될 수 있음 */
interface SingleChoiceQuestion : ChoiceQuestion {
    // 단일 응답만 받을 수 있음
    override fun isValidResponse(questionResponse: QuestionResponse): Boolean {
        if (questionResponse.size != 1) return false
        return super.isValidResponse(questionResponse)
    }

    // 필수 응답인 경우만 keyQuestion이 될 수 있음
    override fun canBeKeyQuestion() = isRequired

    /** 선택지 라우팅의 선택지와 질문의 선택지가 같은지 비교하기 위해 질문의 선택지 집합을 반환 */
    fun getChoiceSet() = choices.getChoiceSet()
}
