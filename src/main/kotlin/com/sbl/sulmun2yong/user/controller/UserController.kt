package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/profile")
    @ResponseBody
    fun getUserProfile(
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User,
    ): UserProfileResponse = userService.getUserProfile(customOAuth2User.name)
}
