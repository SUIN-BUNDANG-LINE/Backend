package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.global.config.oauth2.provider.GoogleUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.KakaoUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.NaverUserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo
import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.config.oauth2.provider.exception.ProviderNotFoundException
import com.sbl.sulmun2yong.global.util.SessionRegistryCleaner
import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.domain.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2Service(
    private val sessionRegistry: SessionRegistry,
    private val userAdapter: UserAdapter,
) : DefaultOAuth2UserService() {
    // TODO: @Transactional 추가
    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User: OAuth2User = super.loadUser(oAuth2UserRequest)

        expireUserSession(sessionRegistry)

        val oAuth2UserInfo = getOAuth2UserInfo(oAuth2UserRequest, oAuth2User)
        val provider = oAuth2UserInfo.getProvider()
        val providerId = oAuth2UserInfo.getProviderId()
        val phoneNumber: String? = oAuth2UserInfo.getPhoneNumber()

        val user: User? = userAdapter.findByProviderAndProviderId(provider, providerId)
        val upsertedUser =
            user?.withUpdatePhoneNumber(phoneNumber)
                ?: User.create(
                    provider = provider,
                    providerId = providerId,
                    phoneNumber = phoneNumber,
                )

        userAdapter.save(upsertedUser)
        return CustomOAuth2User(upsertedUser.id, upsertedUser.role, upsertedUser.nickname, oAuth2User.attributes)
    }

    private fun getOAuth2UserInfo(
        oAuth2UserRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ): OAuth2UserInfo {
        val registrationId = oAuth2UserRequest.clientRegistration.registrationId
        val attributes = oAuth2User.attributes
        return when (registrationId) {
            Provider.GOOGLE.providerName -> GoogleUserInfo(attributes)
            Provider.NAVER.providerName -> NaverUserInfo(attributes)
            Provider.KAKAO.providerName -> KakaoUserInfo(attributes)
            else -> throw ProviderNotFoundException()
        }
    }

    fun expireUserSession(sessionRegistry: SessionRegistry) {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication != null) {
            SessionRegistryCleaner.removeSessionByAuthentication(sessionRegistry, authentication)
        }
    }
}
