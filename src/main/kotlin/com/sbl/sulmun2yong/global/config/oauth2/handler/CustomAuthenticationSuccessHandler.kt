package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.Companion.REDIRECT_URI_PARAM_COOKIE_NAME
import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.global.util.CookieUtils
import com.sbl.sulmun2yong.user.adapter.RefreshTokenAdapter
import com.sbl.sulmun2yong.user.domain.UserRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import java.util.UUID

class CustomAuthenticationSuccessHandler(
    private val frontendBaseUrl: String,
    private val backendBaseUrl: String,
    private val jwtTokenProvider: JwtTokenProvider,
    private val httpCookieOAuth2AuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
    private val refreshTokenAdapter: RefreshTokenAdapter,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        // 인증 정보에서 사용자 정보 추출
        val principal = authentication.principal
        val defaultUserProfile =
            if (principal is CustomOAuth2User) {
                principal.getDefaultUserProfile()
            } else {
                throw IllegalArgumentException("CustomOAuth2User 타입이 아닙니다.")
            }

        // 로그인 후 리디렉트 URL을 쿠키에서 찾거나 기본값으로 설정
        val redirectUriAfterLogin =
            CookieUtils.findCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)?.value
                ?: if (defaultUserProfile.role == UserRole.ROLE_ADMIN) {
                    backendBaseUrl
                } else {
                    frontendBaseUrl
                }

        // OAuth2 인증 요청 쿠키 제거
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response)

        // 토큰 생성
        val accessToken = jwtTokenProvider.createAccessToken(defaultUserProfile)
        val tokenId = UUID.randomUUID()
        val refreshToken = jwtTokenProvider.createRefreshToken(defaultUserProfile.id, tokenId)

        // 리프레시 토큰을 DB에 저장
        val userRefreshToken = jwtTokenProvider.makeRefreshTokenDocument(defaultUserProfile.id, tokenId, refreshToken)
        refreshTokenAdapter.save(userRefreshToken)

        // 쿠키에 토큰 저장
        response.addCookie(jwtTokenProvider.makeAccessTokenCookie(accessToken))
        response.addCookie(jwtTokenProvider.makeRefreshTokenCookie(refreshToken))

        // 로그인 후 리디렉트
        response.sendRedirect(redirectUriAfterLogin)
    }
}
