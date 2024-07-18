package com.sbl.sulmun2yong.global.config.oauth2.provider

import com.sbl.sulmun2yong.global.config.oauth2.provider.exception.NaverAttributeCastingFailedException

class NaverUserInfo(
    attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val nestedAttributes: Map<String, Any> =
        attributes["response"] as? Map<String, Any>
            ?: throw NaverAttributeCastingFailedException()

    override fun getProvider(): Provider = Provider.NAVER

    override fun getProviderId(): String = nestedAttributes["id"] as String

    override fun getNickname(): String = nestedAttributes["name"] as String

    override fun getPhoneNumber(): String = nestedAttributes["mobile"] as String
}
