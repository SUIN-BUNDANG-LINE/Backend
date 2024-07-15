package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.global.config.oauth2.DataFromOAuth2Request
import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.response.UserSession
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAdapter: UserAdapter,
) {
    fun join(dataFromOAuth2Request: DataFromOAuth2Request) {
        userAdapter.join(dataFromOAuth2Request)
    }

    fun getUserSession(dataFromOAuth2Request: DataFromOAuth2Request): UserSession {
        val user = userAdapter.find(dataFromOAuth2Request)
        return UserSession.of(user.id, user.role)
    }
}
