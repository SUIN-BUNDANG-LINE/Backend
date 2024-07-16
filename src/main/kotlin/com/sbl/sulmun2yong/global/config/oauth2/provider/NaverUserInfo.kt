package com.sbl.sulmun2yong.global.config.oauth2.provider

class NaverUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override fun getProvider(): String = "naver"

    override fun getProviderId(): String = attributes["id"] as String

    override fun getNickname(): String = attributes["name"] as String

    override fun getPhoneNumber(): String = attributes["mobile"] as String
}
