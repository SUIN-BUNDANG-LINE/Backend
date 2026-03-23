package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.converter.EncryptedStringConverter
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: Provider,
    @Column(nullable = false)
    val providerId: String,
    @Column(nullable = false, length = 10)
    val nickname: String,
    @Convert(converter = EncryptedStringConverter::class)
    val phoneNumber: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val role: UserRole,
    @Column(nullable = false)
    val isDeleted: Boolean = false,
) : BaseTimeEntity() {
    companion object {
        fun of(user: User): UserEntity =
            UserEntity(
                id = user.id,
                provider = user.provider,
                providerId = user.providerId,
                nickname = user.nickname,
                phoneNumber = user.phoneNumber?.value,
                role = user.role,
            )
    }

    fun toDomain(): User =
        User(
            id = id,
            provider = provider,
            providerId = providerId,
            nickname = nickname,
            phoneNumber = PhoneNumber.createWithNullable(phoneNumber),
            role = role,
        )
}
