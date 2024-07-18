package com.sbl.sulmun2yong.user.domain

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.global.util.RandomNicknameGenerator
import com.sbl.sulmun2yong.user.exception.InvalidUserException
import java.util.UUID

data class User(
    val id: UUID,
    val provider: Provider,
    val providerId: String,
    val nickname: String,
    var phoneNumber: String?,
    val role: UserRole,
) {
    init {
        if (nickname.length !in 2..10) {
            throw InvalidUserException()
        }
        phoneNumber = formatPhoneNumber(phoneNumber)
    }

    companion object {
        fun create(
            provider: Provider,
            providerId: String,
            phoneNumber: String?,
        ) = User(
            id = UUID.randomUUID(),
            provider = provider,
            providerId = providerId,
            nickname = RandomNicknameGenerator.generate(),
            phoneNumber = phoneNumber,
            role = determineRoleByPhoneNumber(phoneNumber),
        )

        private fun determineRoleByPhoneNumber(phoneNumber: String?): UserRole =
            phoneNumber?.let { UserRole.ROLE_AUTHENTICATED_USER } ?: UserRole.ROLE_USER

        private fun formatPhoneNumber(phoneNumber: String?): String? =
            when {
                phoneNumber == null -> null
                phoneNumber.matches(formattedPhoneNumber) -> phoneNumber
                phoneNumber.matches(unformattedPhoneNumber) -> phoneNumber.replaceFirst(phoneNumberCapturePattern, "010-$1-$2")
                else -> throw InvalidUserException()
            }

        private val formattedPhoneNumber = "^010-\\d{4}-\\d{4}$".toRegex()
        private val unformattedPhoneNumber = "^010\\d{8}$".toRegex()
        private val phoneNumberCapturePattern = "^010(\\d{4})(\\d{4})$".toRegex()
    }

    fun withUpdatePhoneNumber(phoneNumber: String?) =
        User(
            id = this.id,
            provider = this.provider,
            providerId = this.providerId,
            nickname = this.nickname,
            phoneNumber = phoneNumber,
            role = takeIf { this.role == UserRole.ROLE_ADMIN }?.role ?: determineRoleByPhoneNumber(phoneNumber),
        )
}
