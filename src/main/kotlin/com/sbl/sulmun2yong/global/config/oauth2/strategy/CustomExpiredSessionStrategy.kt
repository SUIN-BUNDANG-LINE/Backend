package com.sbl.sulmun2yong.global.config.oauth2.strategy

import com.sbl.sulmun2yong.global.util.ResetSession
import org.springframework.http.HttpStatus
import org.springframework.security.web.session.SessionInformationExpiredEvent
import org.springframework.security.web.session.SessionInformationExpiredStrategy

class CustomExpiredSessionStrategy : SessionInformationExpiredStrategy {
    override fun onExpiredSessionDetected(event: SessionInformationExpiredEvent) {
        // 세션 초기화
        ResetSession.reset(event.request, event.response, HttpStatus.UNAUTHORIZED)
    }
}
