package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.global.config.oauth2.provider.GoogleUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.KakaoUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.NaverUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo
import com.sbl.sulmun2yong.global.util.SessionRegistryCleaner
import com.sbl.sulmun2yong.user.dto.request.UserJoinRequest
import com.sbl.sulmun2yong.user.dto.response.UserIdAndRoleResponse
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2Service(
    private val userService: UserService,
    private val sessionRegistry: SessionRegistry,
) : DefaultOAuth2UserService() {
    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User: OAuth2User = super.loadUser(oAuth2UserRequest)

        val oAuth2UserInfo = getOAuth2UserInfo(oAuth2UserRequest, oAuth2User)
        userService.join(UserJoinRequest.of(oAuth2UserInfo))

        // Authentication 객체 가져오기
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication

        if (authentication != null) {
            SessionRegistryCleaner.removeSessionByAuthentication(sessionRegistry, authentication)
        }

        val userIdAndRoleResponse = getUserIdAndRoleResponse(oAuth2UserInfo)
        return CustomOAuth2User(userIdAndRoleResponse, oAuth2User.attributes)
    }

    private fun getOAuth2UserInfo(
        oAuth2UserRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ): OAuth2UserInfo {
        val registrationId = oAuth2UserRequest.clientRegistration.registrationId
        val attributes = oAuth2User.attributes
        return when (registrationId) {
            "google" -> GoogleUserInfo(attributes)
            "naver" -> NaverUserInfo(attributes["response"] as Map<String, Any>)
            "kakao" -> KakaoUserInfo(attributes)
            else -> throw IllegalArgumentException("지원하지 않는 소셜 로그인입니다.")
        }
    }

    private fun getUserIdAndRoleResponse(oAuth2UserInfo: OAuth2UserInfo): UserIdAndRoleResponse {
        val provider = oAuth2UserInfo.getProvider()
        val providerId = oAuth2UserInfo.getProviderId()
        return userService.getUserIdAndRole(provider, providerId)
    }
}
