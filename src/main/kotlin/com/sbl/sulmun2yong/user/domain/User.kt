package com.sbl.sulmun2yong.user.domain

import com.sbl.sulmun2yong.user.exception.NotAPhoneNumberException
import jakarta.validation.constraints.Size
import java.util.UUID

data class User(
    val id: UUID,
    val provider: String,
    val providerId: String,
    @Size(min = 2, max = 10)
    val nickname: String,
    var phoneNumber: String?,
    val role: UserRole,
) {
    init {
        phoneNumber = formatPhoneNumber(phoneNumber)
    }

    private fun formatPhoneNumber(phoneNumber: String?): String? =
        when {
            phoneNumber == null -> null
            phoneNumber.matches("^010-\\d{4}-\\d{4}$".toRegex()) -> phoneNumber
            phoneNumber.matches("^010\\d{8}$".toRegex()) ->
                phoneNumber.replaceFirst("^010(\\d{4})(\\d{4})$".toRegex(), "010-$1-$2")
            else -> throw NotAPhoneNumberException()
        }
}
