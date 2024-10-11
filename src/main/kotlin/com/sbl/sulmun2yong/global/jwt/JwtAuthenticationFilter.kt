package com.sbl.sulmun2yong.global.jwt

import com.sbl.sulmun2yong.global.util.CookieUtils
import com.sbl.sulmun2yong.user.adapter.RefreshTokenAdapter
import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.DefaultUserProfile
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userAdapter: UserAdapter,
    private val userRefreshTokenAdapter: RefreshTokenAdapter,
) : OncePerRequestFilter() {
    companion object {
        private const val ACCESS_TOKEN_COOKIE = "access-token"
        private const val REFRESH_TOKEN_COOKIE = "refresh-token"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val accessToken = CookieUtils.findCookie(request, ACCESS_TOKEN_COOKIE)?.value
        val refreshToken = CookieUtils.findCookie(request, REFRESH_TOKEN_COOKIE)?.value

        // 엑세스 토큰이 유효하면 인증 통과
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            setAuthentication(accessToken, request)
            filterChain.doFilter(request, response)
            return
        }

        // 리프레시 토큰으로 새로운 엑세스 토큰을 발급 시도
        tryRefreshAccessToken(refreshToken).fold(
            onSuccess = { newAccessToken ->
                setAuthentication(newAccessToken, request)
                response.addCookie(jwtTokenProvider.makeAccessTokenCookie(newAccessToken))
            },
            onFailure = {
                println("인증 실패")
            },
        )

        filterChain.doFilter(request, response)
    }

    private fun tryRefreshAccessToken(refreshToken: String?): Result<String> =
        runCatching {
            if (refreshToken == null || jwtTokenProvider.validateToken(refreshToken)) throw IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.")

            val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
            val tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken)
            if (userId == null || tokenId == null) throw IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.")

            val storedToken = userRefreshTokenAdapter.findByTokenIdAndUserId(tokenId, userId)
            if (storedToken != null && storedToken.token == refreshToken) throw IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.")

            val user = userAdapter.getById(userId)
            val defaultUserProfile =
                DefaultUserProfile(
                    id = user.id,
                    role = user.role,
                    nickname = user.nickname,
                )
            jwtTokenProvider.createAccessToken(defaultUserProfile)
        }

    private fun setAuthentication(
        accessToken: String,
        request: HttpServletRequest,
    ) {
        if (SecurityContextHolder.getContext().authentication != null) return
        val user = jwtTokenProvider.getUserFromToken(accessToken) ?: return
        val authenticationToken = UsernamePasswordAuthenticationToken(user, null, user.authorities)
        authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authenticationToken
    }
}
