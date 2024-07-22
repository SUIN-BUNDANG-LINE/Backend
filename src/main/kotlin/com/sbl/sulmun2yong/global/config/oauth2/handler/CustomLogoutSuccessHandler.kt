package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.util.ResetSession
import com.sbl.sulmun2yong.global.util.SessionRegistryCleaner
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler

class CustomLogoutSuccessHandler(
    private val baseUrl: String,
    private val sessionRegistry: SessionRegistry,
) : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        if (authentication !== null) {
            // 세션 레지스트리에서 제거
            SessionRegistryCleaner.removeSessionByAuthentication(sessionRegistry, authentication)
        }
        // 세션 초기화
        ResetSession.reset(request, response)

        // 상태 코드 설정
        response.status = HttpStatus.OK.value()

        // 리디렉트
        response.sendRedirect("$baseUrl/logout/authorizaiton")
    }
}
