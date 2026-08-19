package com.sbl.sulmun2yong.global.config.oauth2.provider

class KakaoUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override fun getProvider(): Provider = Provider.KAKAO

    override fun getProviderId(): String = attributes["id"].toString()

    override fun getNickname(): String = attributes["nickname"] as String

    override fun getPhoneNumber(): String? = null
}
