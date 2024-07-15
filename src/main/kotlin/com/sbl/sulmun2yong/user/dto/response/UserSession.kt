package com.sbl.sulmun2yong.user.dto.response

import com.sbl.sulmun2yong.user.domain.UserRole

class UserSession(
    val id: String = "",
    val role: String = UserRole.ROLE_USER.role,
) {
    companion object {
        fun of(
            id: String,
            role: String,
        ) = UserSession(id, role)
    }
}
