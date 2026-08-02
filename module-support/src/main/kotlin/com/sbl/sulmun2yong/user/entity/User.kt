package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.converter.EncryptedPhoneNumberConverter
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.exception.InvalidUserException
import com.sbl.sulmun2yong.user.util.RandomNicknameGenerator
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
class User(
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
    @Convert(converter = EncryptedPhoneNumberConverter::class)
    var phoneNumber: PhoneNumber?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val role: UserRole,
    @Column(nullable = false)
    val isDeleted: Boolean = false,
) : BaseTimeEntity() {
    init {
        if (nickname.length !in 2..10) {
            throw InvalidUserException()
        }
        if (role == UserRole.ROLE_AUTHENTICATED_USER && phoneNumber == null) {
            throw InvalidUserException()
        }
    }

    fun withUpdatePhoneNumber(phoneNumber: String?): User {
        val phoneNumberData = PhoneNumber.createWithNullable(phoneNumber)
        return User(
            id = this.id,
            provider = this.provider,
            providerId = this.providerId,
            nickname = this.nickname,
            phoneNumber = phoneNumberData,
            role = takeIf { this.role == UserRole.ROLE_ADMIN }?.role ?: determineRoleByPhoneNumber(phoneNumberData),
        )
    }

    companion object {
        fun create(
            provider: Provider,
            providerId: String,
            phoneNumber: String?,
        ): User {
            val phoneNumberData = PhoneNumber.createWithNullable(phoneNumber)
            return User(
                id = UUID.randomUUID(),
                provider = provider,
                providerId = providerId,
                nickname = RandomNicknameGenerator.generate(),
                phoneNumber = phoneNumberData,
                role = determineRoleByPhoneNumber(phoneNumberData),
            )
        }

        private fun determineRoleByPhoneNumber(phoneNumber: PhoneNumber?): UserRole =
            phoneNumber?.let { UserRole.ROLE_AUTHENTICATED_USER } ?: UserRole.ROLE_USER
    }
}
