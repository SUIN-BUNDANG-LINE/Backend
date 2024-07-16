package com.sbl.sulmun2yong.user.dto.response

import com.sbl.sulmun2yong.user.domain.UserRole
import java.util.UUID

class UserIdAndRoleResponse(
    val id: String,
    val role: String,
) {
    companion object {
        fun of(
            id: UUID,
            role: UserRole,
        ) = UserIdAndRoleResponse(id.toString(), role.name)
    }
}
