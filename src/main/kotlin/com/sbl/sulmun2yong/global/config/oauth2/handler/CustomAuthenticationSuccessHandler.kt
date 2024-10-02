package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository
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
    private val cookieDomain: String,
    private val httpCookieOAuth2AuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
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

        val redirectUriAfterLogin =
            CookieUtils.findCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)?.value
                ?: if (defaultUserProfile.role == UserRole.ROLE_ADMIN) {
                    backendBaseUrl
                } else {
                    frontendBaseUrl
                }

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response)

        // 기본 프로필 쿠키 생성
        val cookie = Cookie("user-profile", defaultUserProfile.toBase64Json())
        cookie.path = "/"
        if (cookieDomain != "localhost") {
            cookie.domain = cookieDomain
        }
        response.addCookie(cookie)

        // 리디렉트
        response.sendRedirect(redirectUriAfterLogin)
    }
}
