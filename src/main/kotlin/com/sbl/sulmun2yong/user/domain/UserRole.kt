package com.sbl.sulmun2yong.user.domain

enum class UserRole(
    val role: String,
) {
    ROLE_USER("ROLE_USER"),
    ROLE_AUTHENTICATED_USER("AUTHENTICATED_USER"),
    ROLE_ADMIN("ROLE_ADMIN"),
}
