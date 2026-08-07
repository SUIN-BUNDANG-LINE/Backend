package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// 인증 서비스 - 단일 기록자: refresh_tokens (+users 는 OAuth2 가입 upsert).
// OAuth2 로그인·JWT 발급·재발급 전담. 게이트웨이는 검증만, auth 는 발급만.
// :support 전체가 아니라 필요한 패키지만 스캔한다 - 전체를 스캔하면 결제 어댑터(TossPaymentsAdapter)
// 같은 남의 도메인 빈까지 만들려다 기동이 깨진다(cofunding·payment 와 같은 방식).
// global.config 는 자기 모듈 것(SecurityConfig·oauth2)과 :support 의 EncryptionConfig 를 함께 잡는다
// - User 엔티티의 전화번호 컨버터가 BytesEncryptor 를 요구하기 때문.
@SpringBootApplication(
    scanBasePackages = [
        "com.sbl.sulmun2yong.user",
        "com.sbl.sulmun2yong.global.config",
        "com.sbl.sulmun2yong.global.jwt",
        "com.sbl.sulmun2yong.global.util",
        "com.sbl.sulmun2yong.global.test",
    ],
)
@EnableJpaRepositories(basePackages = ["com.sbl.sulmun2yong.user.repository"])
@EntityScan(basePackages = ["com.sbl.sulmun2yong.user.entity"])
class AuthApplication

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
