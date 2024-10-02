package com.sbl.sulmun2yong.survey.domain.question.choice

import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidChoiceException

/** 질문에 포함될 선택지 목록 */
data class Choices(
    val standardChoices: List<Choice.Standard>,
    /** 기타 선택지 허용 여부 */
    val isAllowOther: Boolean,
) {
    companion object {
        const val MAX_SIZE = 20
    }

    init {
        if (standardChoices.isEmpty()) throw InvalidChoiceException()
        if (standardChoices.size > MAX_SIZE) throw InvalidChoiceException()
    }

    /** 응답이 선택지에 포함되는지 확인 */
    fun isContains(responseDetail: ResponseDetail): Boolean {
        if (responseDetail.isOther) return isAllowOther
        return standardChoices.contains(Choice.Standard(responseDetail.content))
    }

    /** 선택지 기반 라우팅의 선택지와 같은지 비교하기 위해 선택지 집합을 얻는다. */
    fun getChoiceSet() = if (isAllowOther) standardChoices.toSet() + Choice.Other else standardChoices.toSet()

    fun isUnique() = standardChoices.size == standardChoices.distinct().size
}
