package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.global.config.oauth2.OAuth2UserInfoDTO
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.entity.UserDocument
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAdapter(
    private val userRepository: UserRepository,
) {
    fun join(oAuth2UserInfoDTO: OAuth2UserInfoDTO) {
        val provider = oAuth2UserInfoDTO.provider
        val providerId = oAuth2UserInfoDTO.providerId
        val existingUser = userRepository.findByProviderAndProviderId(provider, providerId)

        if (existingUser.isPresent) {
            return
        }
        userRepository.save(oAuth2UserInfoDTO.toDocument())
    }

    private fun OAuth2UserInfoDTO.toDocument() =
        UserDocument(
            id = UUID.randomUUID().toString(),
            provider = this.provider,
            providerId = this.providerId,
            phoneNumber = this.phoneNumber,
            role = if (this.phoneNumber.isEmpty()) UserRole.ROLE_USER.role else UserRole.ROLE_AUTHENTICATED_USER.role,
        )

    fun find(
        provider: String,
        providerId: String,
    ): User =
        userRepository
            .findByProviderAndProviderId(provider, providerId)
            .orElseThrow { IllegalArgumentException("가입되지 않은 회원입니다.") }
            .toDomain()

    fun find(id: String): User =
        userRepository
            .findById(UUID.fromString(id))
            .orElseThrow { IllegalArgumentException("존재하지 않는 회원입니다.") }
            .toDomain()

    private fun UserDocument.toDomain() =
        User(
            id = this.id,
            provider = this.provider,
            providerId = this.providerId,
            nickname = this.nickname,
            phoneNumber = this.phoneNumber,
            role = this.role,
        )
}
