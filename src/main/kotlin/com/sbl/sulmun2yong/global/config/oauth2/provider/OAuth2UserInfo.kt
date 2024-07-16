package com.sbl.sulmun2yong.global.config.oauth2.provider

interface OAuth2UserInfo {
    fun getProvider(): String

    fun getProviderId(): String

    fun getNickname(): String

    fun getPhoneNumber(): String
}
