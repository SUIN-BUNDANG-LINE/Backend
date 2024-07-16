package com.sbl.sulmun2yong.user.dto.request

import com.sbl.sulmun2yong.global.config.oauth2.provider.OAuth2UserInfo
import com.sbl.sulmun2yong.global.util.RandomNicknameGenerator
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.entity.UserDocument
import java.util.UUID

data class UserJoinRequest(
    val provider: String = "",
    val providerId: String = "",
    val phoneNumber: String = "",
) {
    companion object {
        fun of(oAuth2UserInfo: OAuth2UserInfo) =
            UserJoinRequest(
                provider = oAuth2UserInfo.getProvider(),
                providerId = oAuth2UserInfo.getProviderId(),
                phoneNumber = oAuth2UserInfo.getPhoneNumber(),
            )
    }

    fun toDocument() =
        UserDocument(
            id = UUID.randomUUID(),
            provider = this.provider,
            providerId = this.providerId,
            phoneNumber = this.phoneNumber,
            nickname = RandomNicknameGenerator.generate(),
            role = if (this.phoneNumber.isEmpty()) UserRole.ROLE_USER else UserRole.ROLE_AUTHENTICATED_USER,
        )
}
