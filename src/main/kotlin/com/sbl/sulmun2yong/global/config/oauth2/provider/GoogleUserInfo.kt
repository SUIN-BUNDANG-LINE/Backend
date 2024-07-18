package com.sbl.sulmun2yong.global.config.oauth2.provider

class GoogleUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override fun getProvider(): Provider = Provider.GOOGLE

    override fun getProviderId(): String = attributes["sub"] as String

    override fun getNickname(): String = attributes["name"] as String

    override fun getPhoneNumber(): String? = null
}
