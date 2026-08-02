package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.user.dto.DefaultUserProfile
import com.sbl.sulmun2yong.user.repository.RefreshTokenRepository
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Service

// refresh 쿠키로 새 access 토큰을 발급한다(명시적 재발급).
// 게이트웨이 도입 후 access 자동 재발급(구 JwtAuthenticationFilter)을 대체하는 엔드포인트 로직.
@Service
class RefreshTokenService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    // 유효한 refresh 이면 새 access 토큰, 무효면 null.
    fun reissue(refreshToken: String?): String? {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) return null

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken) ?: return null
        val tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken) ?: return null

        // DB 에 저장된 refresh 와 일치해야 유효(없거나 값이 다르면 무효 — 탈취·재사용 방어).
        val stored = refreshTokenRepository.findByIdAndUserId(tokenId, userId).orElse(null) ?: return null
        if (stored.token != refreshToken) return null

        val user = userRepository.findById(userId).orElse(null) ?: return null
        return jwtTokenProvider.createAccessToken(
            DefaultUserProfile(user.id, user.nickname, user.role),
        )
    }
}
