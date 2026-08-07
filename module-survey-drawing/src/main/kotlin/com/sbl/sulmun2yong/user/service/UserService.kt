package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import com.sbl.sulmun2yong.user.exception.UserNotFoundException
import com.sbl.sulmun2yong.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getUserProfile(id: UUID): UserProfileResponse {
        val user = userRepository.findById(id).orElseThrow { UserNotFoundException() }
        return UserProfileResponse.of(user)
    }
}
