package com.sbl.sulmun2yong.global.filter

import com.sbl.sulmun2yong.global.jwt.JwtValidator
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

// 모든 요청에 적용되는 인증 관문.
// 쿠키 access-token 을 검증해 X-User-Id 헤더로 변환하고, 무효면 401 로 차단한다.
// 공개 경로(로그인·리프레시·OAuth2 콜백)는 검증 없이 통과시킨다.
@Component
class JwtAuthGlobalFilter(
    private val jwtValidator: JwtValidator,
) : GlobalFilter,
    Ordered {
    companion object {
        private const val ACCESS_TOKEN_COOKIE = "access-token"
        private const val USER_ID_HEADER = "X-User-Id"
        private const val USER_ROLE_HEADER = "X-User-Role"

        // /api/v1/payments/webhook: 토스 서버가 호출 - 쿠키가 없으므로 JWT 검증 면제(비밀 헤더는 부착됨)
        private val PUBLIC_PREFIXES =
            listOf(
                "/auth/",
                "/login/oauth2/",
                "/oauth2/",
                "/management/",
                "/api/v1/payments/webhook",
                "/payments/",
                "/api/v1/payments/checkout-info",
                "/api/v1/payments/success",
                "/api/v1/payments/fail",
                // 테스트 JWT 발급(로그인의 대체) - 조건부 빈이라 프로덕션에선 404
                "/api/v1/test/token",
                // 초대 페이지(정적) - 링크 소지가 곧 입장 자격. API 호출은 페이지 안 로그인 후.
                "/co-fundings/",
            )
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val path = exchange.request.path.value()

        if (PUBLIC_PREFIXES.any { path.startsWith(it) }) {
            // 공개 경로라도 클라이언트가 위조한 X-User-Id 는 제거하고 통과(신뢰 경계 방어).
            return chain.filter(exchange.mutate().request(stripUserId(exchange)).build())
        }

        val token =
            exchange.request.cookies
                .getFirst(ACCESS_TOKEN_COOKIE)
                ?.value
        val claims = token?.let { jwtValidator.extractUserId(it) }

        if (claims == null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val mutated =
            exchange.request
                .mutate()
                .headers {
                    it.set(USER_ID_HEADER, claims.userId)
                    it.set(USER_ROLE_HEADER, claims.role)
                }.build()
        return chain.filter(exchange.mutate().request(mutated).build())
    }

    private fun stripUserId(exchange: ServerWebExchange) =
        exchange.request
            .mutate()
            .headers { it.remove(USER_ID_HEADER) }
            .headers { it.remove(USER_ROLE_HEADER) }
            .build()

    override fun getOrder() = -1 // 다른 필터보다 먼저 — 인증이 최우선
}
