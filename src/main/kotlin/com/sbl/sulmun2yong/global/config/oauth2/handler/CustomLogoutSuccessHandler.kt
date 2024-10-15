package com.sbl.sulmun2yong.global.config.oauth2.handler

import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider.Companion.REFRESH_TOKEN_COOKIE
import com.sbl.sulmun2yong.global.util.CookieUtils
import com.sbl.sulmun2yong.user.adapter.RefreshTokenAdapter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler

class CustomLogoutSuccessHandler(
    private val frontEndBaseUrl: String,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenAdapter: RefreshTokenAdapter,
) : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        // 리프레시 토큰을 DB에서 삭제
        CookieUtils.findCookie(request, REFRESH_TOKEN_COOKIE)?.let {
            val tokenId = jwtTokenProvider.getTokenIdFromToken(it.value)
            val userId = jwtTokenProvider.getUserIdFromToken(it.value)
            if (tokenId != null && userId != null) refreshTokenAdapter.deleteByTokenIdAndUserId(tokenId, userId)
        }

        // 쿠키에서 토큰 제거
        response.addCookie(jwtTokenProvider.makeExpiredAccessTokenCookie())
        response.addCookie(jwtTokenProvider.makeExpiredRefreshTokenCookie())

        // 리디렉트
        response.sendRedirect("$frontEndBaseUrl/")
    }
}
