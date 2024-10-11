package com.sbl.sulmun2yong.user.dto

import com.sbl.sulmun2yong.user.domain.UserRole
import java.util.UUID

data class DefaultUserProfile(
    val id: UUID,
    val nickname: String,
    val role: UserRole,
)
