package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.global.config.oauth2.OAuth2UserInfoDTO
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.entity.UserDocument
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAdapter(
    private val userRepository: UserRepository,
) {
    fun join(OAuth2UserInfoDTO: OAuth2UserInfoDTO) {
        val provider = OAuth2UserInfoDTO.provider
        val providerId = OAuth2UserInfoDTO.providerId
        val existingMember = userRepository.findByProviderAndProviderId(provider, providerId)

        if (existingMember.isPresent) {
            return
        }
        userRepository.save(OAuth2UserInfoDTO.toDocument())
    }

    private fun OAuth2UserInfoDTO.toDocument() =
        UserDocument(
            id = UUID.randomUUID().toString(),
            provider = this.provider,
            providerId = this.providerId,
            nickname = this.nickname,
        )

    fun find(OAuth2UserInfoDTO: OAuth2UserInfoDTO): User =
        userRepository
            .findByProviderAndProviderId(OAuth2UserInfoDTO.provider, OAuth2UserInfoDTO.providerId)
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
