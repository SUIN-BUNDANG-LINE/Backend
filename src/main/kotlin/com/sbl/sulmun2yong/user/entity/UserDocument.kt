package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "users")
data class UserDocument(
    @Id
    val id: UUID,
    val provider: String,
    val providerId: String,
    val nickname: String,
    val phoneNumber: String,
    val role: UserRole,
    val isDeleted: Boolean = false,
) : BaseTimeDocument() {
    companion object {
        fun of(user: User): UserDocument =
            UserDocument(
                id = UUID.randomUUID(),
                provider = user.provider,
                providerId = user.providerId,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber,
                role = user.role,
            )
    }

    fun toDomain(): User =
        User(
            id = id,
            provider = provider,
            providerId = providerId,
            nickname = nickname,
            phoneNumber = phoneNumber,
            role = role,
        )
}
