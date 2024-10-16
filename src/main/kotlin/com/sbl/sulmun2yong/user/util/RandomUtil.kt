package com.sbl.sulmun2yong.user.util

import com.sbl.sulmun2yong.global.util.DateUtil
import java.util.Date

object RandomUtil {
    /** 현재 날짜 기준 min일과 max일 사이의 무작위 날짜를 반환 */
    fun getRandomDate(
        min: Int,
        max: Int,
        noMin: Boolean = true,
    ): Date = DateUtil.getDateAfterDay(date = DateUtil.getCurrentDate(noMin = noMin), day = (min..max).random())
}
