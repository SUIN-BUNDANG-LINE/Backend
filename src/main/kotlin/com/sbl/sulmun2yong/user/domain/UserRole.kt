package com.sbl.sulmun2yong.user.domain

enum class UserRole(
    val role: String,
) {
    ROLE_USER("ROLE_USER"),
    ROLE_AUTHORIZED_USER("ROLE_AUTHORIZED_USER"),
    ROLE_ADMIN("ROLE_ADMIN"),
}
