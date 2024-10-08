package com.sbl.sulmun2yong.global.config.oauth2.strategy

import com.sbl.sulmun2yong.global.util.ResetSession
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.web.session.InvalidSessionStrategy

class CustomInvalidSessionStrategy(
    val cookieDomain: String,
) : InvalidSessionStrategy {
    override fun onInvalidSessionDetected(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        // 세션 초기화
        ResetSession.reset(request, response, cookieDomain)

        // 상태 코드 설정
        response.status = HttpStatus.UNAUTHORIZED.value()
    }
}
