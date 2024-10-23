package com.sbl.sulmun2yong.user.util.dummy

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.util.RandomNicknameGenerator
import java.util.UUID

object RandomUserGenerateUtil {
    fun generateRandomUser() =
        User(
            id = UUID.randomUUID(),
            provider = listOf(Provider.GOOGLE, Provider.KAKAO).random(),
            providerId = UUID.randomUUID().toString(),
            nickname = RandomNicknameGenerator.generate(),
            phoneNumber = null,
            role = UserRole.ROLE_USER,
        )
}
