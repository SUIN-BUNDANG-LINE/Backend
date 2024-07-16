package com.sbl.sulmun2yong.global.config

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
            response.status = HttpServletResponse.SC_OK
            response.sendRedirect("/")
            return
        }

        // 세션 무효화
        request.session.invalidate()

        // 세션 레지스트리에서 제거
        sessionRegistry
            .getAllSessions(authentication.principal, false)
            .forEach { sessionInformation ->
                sessionRegistry.removeSessionInformation(sessionInformation.sessionId)
            }

        response.status = HttpServletResponse.SC_OK

        // 리디렉션
        response.sendRedirect("/")
    }
}
