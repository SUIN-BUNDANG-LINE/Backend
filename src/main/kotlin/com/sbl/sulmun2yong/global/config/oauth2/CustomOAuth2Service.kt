package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.global.config.oauth2.provider.GoogleUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.KakaoUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.NaverUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo
import com.sbl.sulmun2yong.user.dto.request.UserJoinRequest
import com.sbl.sulmun2yong.user.dto.response.UserSessionResponse
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2Service(
    private val userService: UserService,
) : DefaultOAuth2UserService() {
    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User: OAuth2User = super.loadUser(oAuth2UserRequest)

        val oAuth2UserInfo = getOAuth2UserInfo(oAuth2UserRequest, oAuth2User)

        userService.join(UserJoinRequest.of(oAuth2UserInfo))

        val provider = oAuth2UserInfo.getNickname()
        val providerId = oAuth2UserInfo.getProviderId()
        val userSessionResponse: UserSessionResponse = userService.getUserSession(provider, providerId)

        return CustomOAuth2User(userSessionResponse, oAuth2User.attributes)
    }

    private fun getOAuth2UserInfo(
        oAuth2UserRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ): OAuth2UserInfo {
        val registrationId = oAuth2UserRequest.clientRegistration.registrationId
        val attributes = oAuth2User.attributes
        return when (registrationId) {
            "google" -> {
                println("구글 로그인 요청")
                GoogleUserInfo(attributes)
            }

            "naver" -> {
                println("네이버 로그인 요청")
                NaverUserInfo(attributes["response"] as Map<String, Any>)
            }

            "kakao" -> {
                println("카카오 로그인 요청")
                KakaoUserInfo(attributes)
            }

            else -> {
                throw IllegalArgumentException("지원하지 않는 소셜 로그인입니다.")
            }
        }
    }
}
