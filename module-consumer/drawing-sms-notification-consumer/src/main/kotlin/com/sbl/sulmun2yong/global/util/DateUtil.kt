package com.sbl.sulmun2yong.global.util

import java.util.Calendar
import java.util.Date

object DateUtil {
    /** 현재 시간 (기본: 초 단위 이하 제거) */
    fun getCurrentDate(
        noMin: Boolean = false,
        noSec: Boolean = true,
        noMilliSecond: Boolean = true,
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        if (noMin) calendar.set(Calendar.MINUTE, 0)
        if (noSec) calendar.set(Calendar.SECOND, 0)
        if (noMilliSecond) calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /** {day} 뒤의 Date를 가져온다. (기본: 초 단위 이하 제거, 현재 날짜의 60일 뒤) */
    fun getDateAfterDay(
        date: Date = getCurrentDate(),
        day: Int = 60,
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DATE, day)
        return calendar.time
    }
}
