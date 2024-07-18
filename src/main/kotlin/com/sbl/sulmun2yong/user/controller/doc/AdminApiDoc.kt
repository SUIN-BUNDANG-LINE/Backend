package com.sbl.sulmun2yong.user.controller.doc

import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Admin", description = "관리자 API")
@RequestMapping("/admin")
interface AdminApiDoc {
    @Operation(summary = "로그인한 사용자 조회")
    @GetMapping("/sessions/logged-in-users")
    fun getAllLoggedInUsers(): ResponseEntity<UserSessionsResponse>
}
