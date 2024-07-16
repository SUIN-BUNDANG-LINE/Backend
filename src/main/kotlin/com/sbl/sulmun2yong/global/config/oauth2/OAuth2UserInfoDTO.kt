package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo

data class OAuth2UserInfoDTO(
    val provider: String = "",
    val providerId: String = "",
    val nickname: String = "",
) {
    companion object {
        fun of(oAuth2UserInfo: OAuth2UserInfo) =
            OAuth2UserInfoDTO(
                provider = oAuth2UserInfo.getProvider(),
                providerId = oAuth2UserInfo.getProviderId(),
            )
    }
}
