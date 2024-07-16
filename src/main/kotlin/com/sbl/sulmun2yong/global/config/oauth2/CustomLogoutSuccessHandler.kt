package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.util.SessionRegistryCleaner
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler

class CustomLogoutSuccessHandler(
    private val sessionRegistry: SessionRegistry,
) : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        // 쿠키 삭제
        val cookie = Cookie("JSESSIONID", null)
        cookie.path = "/"
        cookie.maxAge = 0
        response.addCookie(cookie)

        if (authentication == null) {
            redirect(response)
            return
        }
        // 세션 레지스트리에서 제거
        SessionRegistryCleaner.removeSessionByAuthentication(sessionRegistry, authentication)

        // 세션 무효화
        request.session.invalidate()

        // 리디렉션
        redirect(response)
    }

    private fun redirect(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_OK
        response.sendRedirect("/")
    }
}
