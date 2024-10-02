package com.sbl.sulmun2yong.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

object ResetSession {
    fun reset(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        // 세션 무효화
        request.session.invalidate()

        // 쿠키 삭제
        val expiredJsessionIdCookie = Cookie("JSESSIONID", null)
        expiredJsessionIdCookie.path = "/"
        expiredJsessionIdCookie.maxAge = 0
        response.addCookie(expiredJsessionIdCookie)

        val expiredUserProfileCookie = Cookie("user-profile", null)
        expiredUserProfileCookie.path = "/"
        expiredUserProfileCookie.maxAge = 0
        response.addCookie(expiredUserProfileCookie)
    }
}
