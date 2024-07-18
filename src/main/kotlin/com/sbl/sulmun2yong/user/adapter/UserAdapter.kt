package com.sbl.sulmun2yong.user.adapter

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
    fun join(user: User) {
        val provider = user.provider
        val providerId = user.providerId
        userRepository.save(UserDocument.of(user))
    }

    fun findByProviderAndProviderId(
        provider: String,
        providerId: String,
    ): User? {
        val userDocument = userRepository.findByProviderAndProviderId(provider, providerId)
        return if (userDocument.isPresent) {
            userDocument.get().toDomain()
        } else {
            null
        }
    }

    fun findById(id: UUID): User =
        userRepository
            .findById(id)
            .orElseThrow { UserNotFoundException() }
            .toDomain()

    fun countByNickname(nickname: String): Long =
        userRepository
            .countByNickname(nickname)
}
