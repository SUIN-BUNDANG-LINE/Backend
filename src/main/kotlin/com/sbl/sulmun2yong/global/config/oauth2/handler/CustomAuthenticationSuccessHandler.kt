package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

class CustomAuthenticationSuccessHandler(
    private val baseUrl: String,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        // 상태 코드 설정
        response.status = HttpStatus.OK.value()

        val principal = authentication.principal
        val defaultUserProfile =
            if (principal is CustomOAuth2User) {
                principal.getDefaultUserProfile()
            } else {
                throw IllegalArgumentException("CustomOAuth2User 타입이 아닙니다.")
            }

        // 기본 프로필 쿠키 생성
        val cookie = Cookie("user-profile", defaultUserProfile.toBase64Json())
        cookie.path = "/"
        response.addCookie(cookie)

        // 리디렉트
        response.sendRedirect("$baseUrl/")
    }
}
