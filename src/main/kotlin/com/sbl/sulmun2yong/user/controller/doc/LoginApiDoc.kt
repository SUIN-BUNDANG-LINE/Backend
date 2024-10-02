package com.sbl.sulmun2yong.user.controller.doc

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Tag(name = "Login", description = "로그인 API")
interface LoginApiDoc {
    @Operation(summary = "oauth 로그인")
    @GetMapping("/login/{provider}")
    @ResponseBody
    fun login(
        @PathVariable provider: String,
        @RequestParam("redirect_path") redirectPathAfterLogin: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Any>
}
