package com.sbl.sulmun2yong.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.encrypt.AesBytesEncryptor
import org.springframework.security.crypto.encrypt.BytesEncryptor

// 전화번호 등 개인정보 컬럼 암호화 키 - EncryptedPhoneNumberConverter·EncryptionUtils 의 짝.
// 컨버터가 :support 의 User·DrawingHistory 엔티티에 붙어 있으므로, 이 엔티티를 스캔하는 모든
// 실행단위(web·auth 등)가 이 빈을 필요로 한다 - 그래서 :support 에 둔다.
// (컨슈머 계열은 :support 대신 :common 을 쓰므로 각자 사본을 유지한다)
@Configuration
class EncryptionConfig(
    @Value("\${encryption.password}") private val password: String,
    @Value("\${encryption.salt}") private val salt: String,
) {
    @Bean
    fun aesBytesEncryptor(): BytesEncryptor = AesBytesEncryptor(password, salt)
}
