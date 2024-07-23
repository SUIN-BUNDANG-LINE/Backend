package com.sbl.sulmun2yong.global.config.oauth2.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

class CustomAuthenticationSuccessHandler(
    private val baseUrl: String,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: org.springframework.security.core.Authentication?,
    ) {
        // 상태 코드 설정
        response.status = HttpStatus.OK.value()

        // 리디렉트
        response.sendRedirect("$baseUrl/")
    }
}
