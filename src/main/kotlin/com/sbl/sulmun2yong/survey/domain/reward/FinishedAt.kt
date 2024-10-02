package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidFinishedAtException
import java.util.Calendar
import java.util.Date

/** 설문 종료일, 1시간 단위(분 단위 이하 0) */
data class FinishedAt(
    val value: Date,
) {
    init {
        require(isMinuteAndBelowZero()) { throw InvalidFinishedAtException() }
    }

    private fun isMinuteAndBelowZero(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = value
        return calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0 && calendar.get(Calendar.MILLISECOND) == 0
    }
}
