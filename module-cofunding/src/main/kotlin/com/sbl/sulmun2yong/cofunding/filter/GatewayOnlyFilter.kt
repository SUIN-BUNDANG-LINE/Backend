package com.sbl.sulmun2yong.cofunding.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// 게이트웨이 우회 직접 접근 차단 — 게이트웨이만 아는 공유 비밀 헤더를 검사한다(web 필터와 동일 규칙).
// 이 헤더가 없거나 틀리면(게이트웨이를 안 거친 요청) 403.
// 완전한 격리는 네트워크(NetworkPolicy·mTLS)로 하고, 이 필터는 애플리케이션 레벨 방어선이다.
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
