package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// 사용자 프로필 조회 - 사용자의 주인이 auth 라 여기서 제공한다(게이트웨이가 /api/v1/user/** 를 라우팅).
// 게이트웨이가 검증·주입한 X-User-Id 헤더만 신뢰한다 - 이 모듈에는 @LoginUser 리졸버가 없다.
@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/profile")
    fun getUserProfile(
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<UserProfileResponse> = ResponseEntity.ok(userService.getUserProfile(userId))
}
