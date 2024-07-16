package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import org.springframework.stereotype.Service

@Service
class AdminService {
    fun getAllLoggedInUsers(allUsers: List<Any>): UserSessionsResponse = UserSessionsResponse.of()
}
