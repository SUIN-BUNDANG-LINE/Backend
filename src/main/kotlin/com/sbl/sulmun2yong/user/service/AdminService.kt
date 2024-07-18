package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import org.springframework.stereotype.Service

@Service
class AdminService {
    fun getAllLoggedInUsers(customOAuth2Users: List<Any>): UserSessionsResponse = UserSessionsResponse(customOAuth2Users)
}
