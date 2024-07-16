package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.user.domain.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class UserDocument(
    @Id
    val id: String,
    val provider: String,
    val providerId: String,
    val nickname: String = "",
    val phoneNumber: String,
    val role: String = UserRole.ROLE_USER.role,
    val isDeleted: Boolean = false,
) : BaseTimeDocument()
