package com.sbl.sulmun2yong.user.dto.response

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import java.util.UUID

class UserProfileResponse(
    val id: UUID,
    var provider: Provider,
    val nickname: String,
    val phoneNumber: String?,
    val role: UserRole,
) {
    companion object {
        fun of(user: User) =
            UserProfileResponse(
                id = user.id,
                provider = user.provider,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber?.value,
                role = user.role,
            )
    }
}
