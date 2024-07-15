package com.sbl.sulmun2yong.user.domain

data class User(
    val id: String = "",
    val provider: String = "",
    val providerId: String = "",
    val nickname: String = "",
    val phoneNumber: String = "",
    val role: String = UserRole.ROLE_USER.role,
)
