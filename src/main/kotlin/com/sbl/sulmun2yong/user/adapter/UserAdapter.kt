package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.entity.UserDocument
import com.sbl.sulmun2yong.user.exception.UserNotFoundException
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAdapter(
    private val userRepository: UserRepository,
) {
    fun save(user: User) {
        userRepository.save(UserDocument.of(user))
    }

    fun getById(id: UUID): User =
        userRepository
            .findById(id)
            .orElseThrow { UserNotFoundException() }
            .toDomain()

    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): User? =
        userRepository
            .findByProviderAndProviderId(provider, providerId)
            .map { it.toDomain() }
            .orElse(null)
}
