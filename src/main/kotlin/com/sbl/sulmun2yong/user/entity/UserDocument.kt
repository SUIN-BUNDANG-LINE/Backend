package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "users")
class UserDocument private constructor(
    @Id
    val id: UUID,
    val provider: Provider,
    val providerId: String,
    val nickname: String,
    val phoneNumber: String?,
    val role: UserRole,
    val isDeleted: Boolean = false,
) : BaseTimeDocument() {
    companion object {
        fun of(user: User): UserDocument =
            UserDocument(
                id = user.id,
                provider = user.provider,
                providerId = user.providerId,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber.toString(),
                role = user.role,
            )
    }

    fun toDomain(): User =
        User(
            id = id,
            provider = provider,
            providerId = providerId,
            nickname = nickname,
            phoneNumber = PhoneNumber.create(phoneNumber),
            role = role,
        )
}
