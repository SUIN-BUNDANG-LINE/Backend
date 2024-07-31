package com.sbl.sulmun2yong.survey.domain.question.choice

import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidChoiceException

/** 질문에 포함될 선택지 목록 */
data class Choices(
    val standardChoices: List<Choice.Standard>,
    /** 기타 선택지 허용 여부 */
    val isAllowOther: Boolean,
) {
    init {
        if (standardChoices.isEmpty()) throw InvalidChoiceException()
        if (standardChoices.size != standardChoices.distinct().size) throw InvalidChoiceException()
    }

    fun isContains(responseDetail: ResponseDetail): Boolean {
        if (responseDetail.isOther) return isAllowOther
        return standardChoices.contains(Choice.Standard(responseDetail.content))
    }

    fun getChoiceSet() = if (isAllowOther) standardChoices.toSet() + Choice.Other else standardChoices.toSet()
}
