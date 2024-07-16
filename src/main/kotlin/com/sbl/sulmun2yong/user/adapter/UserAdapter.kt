package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.user.domain.User
import com.sbl.sulmun2yong.user.dto.request.UserJoinRequest
import com.sbl.sulmun2yong.user.exception.UserNotFoundException
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAdapter(
    private val userRepository: UserRepository,
) {
    fun join(userJoinRequest: UserJoinRequest) {
        val provider = userJoinRequest.provider
        val providerId = userJoinRequest.providerId
        val existingUser = userRepository.findByProviderAndProviderId(provider, providerId)

        if (existingUser.isPresent) {
            return
        }

        userRepository.save(userJoinRequest.toDocument())
    }

    fun find(
        provider: String,
        providerId: String,
    ): User =
        userRepository
            .findByProviderAndProviderId(provider, providerId)
            .orElseThrow { UserNotFoundException() }
            .toDomain()

    fun find(id: String): User =
        userRepository
            .findById(UUID.fromString(id))
            .orElseThrow { UserNotFoundException() }
            .toDomain()
}
