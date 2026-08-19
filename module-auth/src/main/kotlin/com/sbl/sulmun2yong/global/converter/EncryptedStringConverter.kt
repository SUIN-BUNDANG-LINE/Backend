package com.sbl.sulmun2yong.global.converter

import com.sbl.sulmun2yong.global.util.EncryptionUtils
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component

// autoApply=false: 명시적으로 @Convert를 붙인 필드에만 적용
@Converter
@Component
class EncryptedStringConverter(
    private val encryptionUtils: EncryptionUtils,
) : AttributeConverter<String?, String?> {
    override fun convertToDatabaseColumn(attribute: String?): String? = attribute?.let { encryptionUtils.encrypt(it) }

    override fun convertToEntityAttribute(dbData: String?): String? = dbData?.let { encryptionUtils.decrypt(it) }
}
