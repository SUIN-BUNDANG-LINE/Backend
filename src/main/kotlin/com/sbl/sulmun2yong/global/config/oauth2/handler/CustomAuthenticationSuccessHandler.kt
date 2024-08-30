package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.Companion.REDIRECT_URI_PARAM_COOKIE_NAME
import com.sbl.sulmun2yong.global.util.CookieUtils
import com.sbl.sulmun2yong.user.domain.UserRole
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

class CustomAuthenticationSuccessHandler(
    private val frontendBaseUrl: String,
    private val backendBaseUrl: String,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal
        val defaultUserProfile =
            if (principal is CustomOAuth2User) {
                principal.getDefaultUserProfile()
            } else {
                throw IllegalArgumentException("CustomOAuth2User 타입이 아닙니다.")
            }

        val redirectUriCookieValue = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME).value
        val redirectUri =
            if (redirectUriCookieValue == backendBaseUrl && defaultUserProfile.role != UserRole.ROLE_ADMIN) {
                frontendBaseUrl
            } else {
                redirectUriCookieValue
            }

        // 기본 프로필 쿠키 생성
        val cookie = Cookie("user-profile", defaultUserProfile.toBase64Json())
        cookie.path = "/"
        response.addCookie(cookie)

        // 리디렉트
        response.sendRedirect(redirectUri)
    }
}
