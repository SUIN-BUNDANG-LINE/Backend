package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import org.springframework.stereotype.Service

@Service
class AdminService {
    fun getAllLoggedInUsers(allUsers: List<Any>) {
        for (user in allUsers) {
            if (user is CustomOAuth2User) {
                println(user.name)
            }
        }
    }
}
