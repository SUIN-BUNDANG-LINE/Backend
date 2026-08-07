package com.sbl.sulmun2yong.global.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// 게이트웨이가 검증/주입한 X-User-Id / X-User-Role 을 신뢰해 SecurityContext 를 채운다.
// 여기서 JWT 를 다시 검증하지 않는다 - 검증은 게이트웨이가 이미 했다(신뢰 경계 안쪽).
// principal 은 순수 UUID, 권한은 헤더의 role 그대로 — web 은 OAuth2 principal 타입을 모른다(PURE).
@Component
class HeaderAuthenticationFilter : OncePerRequestFilter() {
    companion object {
        private const val USER_ID_HEADER = "X-User-Id"
        private const val USER_ROLE_HEADER = "X-User-Role"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader(USER_ID_HEADER)
        val role = request.getHeader(USER_ROLE_HEADER)

        if (userId != null && role != null && SecurityContextHolder.getContext().authentication == null) {
            val authorities = listOf(SimpleGrantedAuthority(role))
            val authentication = UsernamePasswordAuthenticationToken(UUID.fromString(userId), null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}
