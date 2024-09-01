package com.sbl.sulmun2yong.user.domain

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.util.RandomNicknameGenerator
import com.sbl.sulmun2yong.user.exception.InvalidUserException
import java.util.UUID

data class User(
    val id: UUID,
    val provider: Provider,
    val providerId: String,
    val nickname: String,
    var phoneNumber: PhoneNumber?,
    val role: UserRole,
) {
    init {
        if (nickname.length !in 2..10) {
            throw InvalidUserException()
        }
        if (this.role == UserRole.ROLE_AUTHENTICATED_USER && phoneNumber == null) {
            throw InvalidUserException()
        }
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
}
