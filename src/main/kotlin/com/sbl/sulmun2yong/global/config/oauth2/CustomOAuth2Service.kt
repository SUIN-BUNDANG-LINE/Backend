package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.global.config.oauth2.provider.GoogleUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.KakaoUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.NaverUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo
import com.sbl.sulmun2yong.user.dto.response.UserSession
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2Service(
    private val userService: UserService,
) : DefaultOAuth2UserService() {
    private var dataFromOAuth2Request = DataFromOAuth2Request()

    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User: OAuth2User = super.loadUser(oAuth2UserRequest)

        println(oAuth2User)
        setDataFromOAuth2Request(oAuth2UserRequest, oAuth2User)

        userService.join(dataFromOAuth2Request)

        val userSession: UserSession = userService.getUserSession(dataFromOAuth2Request)

        return CustomOAuth2User(userSession, oAuth2User.attributes)
    }

    private fun setDataFromOAuth2Request(
        userRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ) {
        val registrationId = userRequest.clientRegistration.registrationId
        val attributes = oAuth2User.attributes
        val oAuth2UserInfo: OAuth2UserInfo =
            when (registrationId) {
                "google" -> {
                    println("구글 로그인 요청")
                    GoogleUserInfo(attributes)
                }
                "naver" -> {
                    println("네이버 로그인 요청")
                    println(attributes)
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

        this.dataFromOAuth2Request =
            DataFromOAuth2Request(
                provider = oAuth2UserInfo.getProvider(),
                providerId = oAuth2UserInfo.getProviderId(),
            )
    }
}
