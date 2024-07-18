package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.user.adapter.UserAdapter
import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userAdapter: UserAdapter,
) {
    fun getUserProfile(id: UUID): UserProfileResponse {
        val user = userAdapter.findById(id)
        return UserProfileResponse.of(user)
    }
}
