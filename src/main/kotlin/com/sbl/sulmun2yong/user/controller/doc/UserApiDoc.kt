package com.sbl.sulmun2yong.user.controller.doc

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.user.dto.response.UserProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID

@Tag(name = "User", description = "회원 API")
@RequestMapping("/api/v1/user")
interface UserApiDoc {
    @Operation(summary = "내 정보 조회")
    @GetMapping("/profile")
    @ResponseBody
    fun getUserProfile(
        @LoginUser id: UUID,
    ): ResponseEntity<UserProfileResponse>
}
