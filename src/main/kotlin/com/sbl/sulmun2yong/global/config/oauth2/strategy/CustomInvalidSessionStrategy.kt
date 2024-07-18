package com.sbl.sulmun2yong.global.config.oauth2.strategy

import com.sbl.sulmun2yong.global.util.ResetSession
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.web.session.InvalidSessionStrategy

class CustomInvalidSessionStrategy : InvalidSessionStrategy {
    override fun onInvalidSessionDetected(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        ResetSession.reset(request, response, HttpStatus.UNAUTHORIZED)
    }
}
