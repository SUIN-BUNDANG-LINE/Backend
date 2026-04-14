package com.sbl.sulmun2yong.global.converter

import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component

// PhoneNumber 값 객체 ↔ 암호화된 String 변환
@Converter
@Component
class EncryptedPhoneNumberConverter(
    private val encryptionUtils: EncryptionUtils,
) : AttributeConverter<PhoneNumber?, String?> {
    override fun convertToDatabaseColumn(attribute: PhoneNumber?): String? = attribute?.let { encryptionUtils.encrypt(it.value) }

    override fun convertToEntityAttribute(dbData: String?): PhoneNumber? =
        dbData?.let {
            PhoneNumber.createWithNullable(encryptionUtils.decrypt(it))
        }
}
