package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.global.config.oauth2.OAuth2UserInfoDTO
import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.response.UserSession
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAdapter: UserAdapter,
) {
    fun join(oAuth2UserInfoDTO: OAuth2UserInfoDTO) {
        userAdapter.join(oAuth2UserInfoDTO)
    }

    fun getUserSession(oAuth2UserInfoDTO: OAuth2UserInfoDTO): UserSession {
        val user = userAdapter.find(oAuth2UserInfoDTO)
        return UserSession.of(user.id, user.role)
    }
}
