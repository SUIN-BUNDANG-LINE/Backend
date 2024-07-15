package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.global.config.oauth2.DataFromOAuth2Request
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.entity.UserDocument
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAdapter(
    private val userRepository: UserRepository,
) {
    fun join(dataFromOAuth2Request: DataFromOAuth2Request) {
        val provider = dataFromOAuth2Request.provider
        val providerId = dataFromOAuth2Request.providerId
        val existingMember = userRepository.findByProviderAndProviderId(provider, providerId)

        if (existingMember.isPresent) {
            return
        }
        userRepository.save(dataFromOAuth2Request.toDocument())
    }

    private fun DataFromOAuth2Request.toDocument() =
        UserDocument(
            id = UUID.randomUUID().toString(),
            provider = this.provider,
            providerId = this.providerId,
            nickname = this.nickname,
        )

    fun find(dataFromOAuth2Request: DataFromOAuth2Request): User =
        userRepository
            .findByProviderAndProviderId(dataFromOAuth2Request.provider, dataFromOAuth2Request.providerId)
            .orElseThrow { IllegalArgumentException("가입되지 않은 회원입니다.") }
            .toDomain()

    private fun UserDocument.toDomain() =
        User(
            id = this.id,
            provider = this.provider,
            providerId = this.providerId,
            role = this.role,
            nickname = this.nickname,
        )
}
