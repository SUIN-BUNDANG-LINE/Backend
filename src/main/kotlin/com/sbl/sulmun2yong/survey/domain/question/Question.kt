package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import java.util.UUID

/** 설문의 질문 */
interface Question {
    val id: UUID
    val questionType: QuestionType
    val title: String
    val description: String
    val isRequired: Boolean
    val choices: Choices?

    /** 질문에 유효한 응답인지 검증 */
    fun isValidResponse(questionResponse: QuestionResponse): Boolean

    /** 선택지 라우팅의 keyQuestion이 될 수 있는지 판단 */
    fun canBeKeyQuestion(): Boolean
}
