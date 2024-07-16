package com.sbl.sulmun2yong.user.domain

import java.util.UUID

data class User(
    val id: UUID,
    val provider: String,
    val providerId: String,
    val nickname: String,
    val phoneNumber: String,
    val role: UserRole,
)
