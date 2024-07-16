package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.request.UserJoinRequest
import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import com.sbl.sulmun2yong.user.dto.response.UserSessionResponse
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAdapter: UserAdapter,
) {
    fun join(userJoinRequest: UserJoinRequest) {
        userAdapter.join(userJoinRequest)
    }

    fun getUserSession(
        provider: String,
        providerId: String,
    ): UserSessionResponse {
        val user = userAdapter.find(provider, providerId)
        return UserSessionResponse.of(user.id, user.role)
    }

    fun getUserProfile(id: String): UserProfileResponse {
        val user = userAdapter.find(id)
        return UserProfileResponse.of(user)
    }
}
