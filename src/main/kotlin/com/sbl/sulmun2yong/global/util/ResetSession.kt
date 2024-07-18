package com.sbl.sulmun2yong.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus

class ResetSession {
    companion object {
        fun reset(
            request: HttpServletRequest,
            response: HttpServletResponse,
            status: HttpStatus,
        ) {
            // 세션 무효화
            request.session.invalidate()

            // 쿠키 삭제
            val cookie = Cookie("JSESSIONID", null)
            cookie.path = "/"
            cookie.maxAge = 0
            response.addCookie(cookie)
            // 상태 코드 설정
            response.status = status.value()
        }
    }
}
