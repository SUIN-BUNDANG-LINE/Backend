package com.sbl.sulmun2yong.global.data

import com.sbl.sulmun2yong.global.data.exception.InvalidPhoneNumberException

data class PhoneNumber(
    val value: String,
) {
    init {
        require(value.matches(formattedPhoneNumber)) { throw InvalidPhoneNumberException() }
    }

    companion object {
        fun createWithNullable(phoneNumber: String?): PhoneNumber? =
            when {
                phoneNumber == null -> null
                phoneNumber.matches(formattedPhoneNumber) -> PhoneNumber(phoneNumber)
                phoneNumber.matches(unformattedPhoneNumber) ->
                    PhoneNumber(
                        phoneNumber.replaceFirst(phoneNumberCapturePattern, "010-$1-$2"),
                    )
                else -> throw InvalidPhoneNumberException()
            }

        fun createWithNonNullable(phoneNumber: String): PhoneNumber =
            when {
                phoneNumber.matches(formattedPhoneNumber) -> PhoneNumber(phoneNumber)
                phoneNumber.matches(unformattedPhoneNumber) ->
                    PhoneNumber(
                        phoneNumber.replaceFirst(phoneNumberCapturePattern, "010-$1-$2"),
                    )
                else -> throw InvalidPhoneNumberException()
            }

        private val formattedPhoneNumber = "^010-\\d{4}-\\d{4}$".toRegex()
        private val unformattedPhoneNumber = "^010\\d{8}$".toRegex()
        private val phoneNumberCapturePattern = "^010(\\d{4})(\\d{4})$".toRegex()
    }
}
