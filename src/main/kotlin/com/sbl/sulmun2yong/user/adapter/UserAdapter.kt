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
        userRepository.save(UserDocument.of(user))
    }

    fun findByProviderAndProviderId(
        provider: String,
        providerId: String,
    ): User =
        userRepository
            .findByProviderAndProviderId(provider, providerId)
            .orElseThrow { UserNotFoundException() }
            .toDomain()

    fun findById(id: UUID): User =
        userRepository
            .findById(id)
            .orElseThrow { UserNotFoundException() }
            .toDomain()

    fun countByNicknameRegex(nicknamePattern: String): Long = userRepository.countByNicknameRegex(nicknamePattern)
}
