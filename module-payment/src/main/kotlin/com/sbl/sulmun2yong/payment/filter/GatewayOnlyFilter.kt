package com.sbl.sulmun2yong.payment.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// 게이트웨이 우회 직접 접근 차단 — 게이트웨이만 아는 공유 비밀 헤더를 검사한다(web·cofunding 과 동일 규칙).
// 토스 웹훅·success/fail 리다이렉트도 공개 도메인(게이트웨이)을 거쳐 들어오므로 비밀 헤더가 붙는다
// (웹훅은 쿠키가 없어 게이트웨이 PUBLIC_PREFIXES 에 등록 — JWT 검증만 면제, 비밀 헤더는 부착).
@Component
class GatewayOnlyFilter(
    @Value("\${gateway.secret:local-dev-secret}")
    private val gatewaySecret: String,
) : OncePerRequestFilter() {
    companion object {
        private const val GATEWAY_AUTH_HEADER = "X-Gateway-Auth"
    }

    // 헬스체크·메트릭은 docker-compose healthcheck·Prometheus 가 게이트웨이 우회로 직접 호출하므로 검사에서 제외한다.
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/management")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.getHeader(GATEWAY_AUTH_HEADER) != gatewaySecret) {
            response.status = HttpStatus.FORBIDDEN.value()
            return
        }
        filterChain.doFilter(request, response)
    }
}
