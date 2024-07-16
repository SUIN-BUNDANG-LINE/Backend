package com.sbl.sulmun2yong.user.dto.response

import com.sbl.sulmun2yong.user.domain.User

class UserProfile(
    var provider: String = "",
    var nickname: String = "",
    var phoneNumber: String = "",
    var role: String = "",
) {
    companion object {
        fun of(user: User) =
            UserProfile(
                provider = user.provider,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber,
                role = user.role,
            )
    }
}
