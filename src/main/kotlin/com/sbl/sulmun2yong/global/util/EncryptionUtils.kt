package com.sbl.sulmun2yong.global.util

import org.springframework.security.crypto.encrypt.BytesEncryptor
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class EncryptionUtils(
    private val bytesEncryptor: BytesEncryptor,
) {
    fun encrypt(data: String): String {
        val encryptedBytes = bytesEncryptor.encrypt(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(data: String): String {
        val decodedBytes = Base64.getDecoder().decode(data)
        val decryptedBytes = bytesEncryptor.decrypt(decodedBytes)
        return String(decryptedBytes)
    }
}
