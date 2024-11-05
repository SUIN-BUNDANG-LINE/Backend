package com.sbl.sulmun2yong.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.encrypt.AesBytesEncryptor
import org.springframework.security.crypto.encrypt.BytesEncryptor

@Configuration
class EncryptionConfig(
    @Value("\${encryption.password}") private val password: String,
    @Value("\${encryption.salt}") private val salt: String,
) {
    @Bean
    fun aesBytesEncryptor(): BytesEncryptor = AesBytesEncryptor(password, salt)
}
