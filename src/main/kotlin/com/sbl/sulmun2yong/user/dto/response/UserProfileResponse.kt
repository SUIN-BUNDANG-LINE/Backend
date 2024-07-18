package com.sbl.sulmun2yong.user.dto.response

import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole

class UserProfileResponse(
    var provider: String,
    var nickname: String,
    var phoneNumber: String?,
    var role: UserRole,
) {
    companion object {
        fun of(user: User) =
            UserProfileResponse(
                provider = user.provider,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber,
                role = user.role,
            )
    }
}
