package com.sbl.sulmun2yong.global.test

import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.dto.DefaultUserProfile
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 부하 테스트(k6)용 JWT 발급 엔드포인트.
 *
 * OAuth2 로그인 없이 access token 을 발급받기 위한 용도로, 브라우저 쿠키 복사 과정을 대체한다.
 * 토큰 발급은 auth-service 소관이므로 여기(auth)에 둔다.
 *
 * 프로덕션에서 노출되지 않도록 `test-auth.enabled=true` 일 때만 빈이 등록된다.
 */
@RestController
@RequestMapping("/api/v1/test")
@ConditionalOnProperty(prefix = "test-auth", name = ["enabled"], havingValue = "true")
class TestAuthController(
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @PostMapping("/token")
    fun issueToken(
        @RequestParam(required = false) userId: String?,
        @RequestParam(required = false, defaultValue = "ROLE_AUTHENTICATED_USER") role: UserRole,
        @RequestParam(required = false, defaultValue = "k6tester") nickname: String,
        response: HttpServletResponse,
    ): ResponseEntity<TestTokenResponse> {
        val id = userId?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        val profile = DefaultUserProfile(id = id, nickname = nickname, role = role)

        val accessToken = jwtTokenProvider.createAccessToken(profile)
        response.addCookie(jwtTokenProvider.makeAccessTokenCookie(accessToken))

        return ResponseEntity.ok(TestTokenResponse(accessToken = accessToken, userId = id.toString(), role = role))
    }
}

data class TestTokenResponse(
    val accessToken: String,
    val userId: String,
    val role: UserRole,
)
