package com.sbl.sulmun2yong.global.config.oauth2.strategy

import com.sbl.sulmun2yong.global.util.ResetSession
import org.springframework.http.HttpStatus
import org.springframework.security.web.session.SessionInformationExpiredEvent
import org.springframework.security.web.session.SessionInformationExpiredStrategy

class CustomExpiredSessionStrategy(
    val cookieDomain: String,
) : SessionInformationExpiredStrategy {
    override fun onExpiredSessionDetected(event: SessionInformationExpiredEvent) {
        val request = event.request
        val response = event.response

        // 세션 초기화
        ResetSession.reset(request, response, cookieDomain)

        // 상태 코드 설정
        response.status = HttpStatus.UNAUTHORIZED.value()
    }
}
