package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.user.controller.doc.UserApiDoc
import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/api/v1/user")
class UserController(
    private val userService: UserService,
) : UserApiDoc {
    @GetMapping("/profile")
    @ResponseBody
    override fun getUserProfile(
        @LoginUser id: UUID,
    ): ResponseEntity<UserProfileResponse> = ResponseEntity.ok(userService.getUserProfile(id))
}
