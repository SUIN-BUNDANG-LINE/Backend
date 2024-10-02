package com.sbl.sulmun2yong.global.util

import org.springframework.security.core.Authentication
import org.springframework.security.core.session.SessionRegistry

object SessionRegistryCleaner {
    fun removeSessionByAuthentication(
        sessionRegistry: SessionRegistry,
        authentication: Authentication,
    ) {
        sessionRegistry
            .getAllSessions(authentication.principal, false)
            .forEach { sessionInformation ->
                sessionRegistry.removeSessionInformation(sessionInformation.sessionId)
            }
    }
}
