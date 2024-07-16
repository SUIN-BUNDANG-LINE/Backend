package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.global.config.oauth2.OAuth2UserInfoDTO
import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.response.UserProfile
import com.sbl.sulmun2yong.user.dto.response.UserSession
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAdapter: UserAdapter,
) {
    fun join(oAuth2UserInfoDTO: OAuth2UserInfoDTO) {
        userAdapter.join(oAuth2UserInfoDTO)
    }

    fun getUserSession(
        provider: String,
        providerId: String,
    ): UserSession {
        val user = userAdapter.find(provider, providerId)
        return UserSession.of(user.id, user.role)
    }

    fun getUserProfile(id: String): UserProfile {
        val user = userAdapter.find(id)
        return UserProfile.of(user)
    }
}
