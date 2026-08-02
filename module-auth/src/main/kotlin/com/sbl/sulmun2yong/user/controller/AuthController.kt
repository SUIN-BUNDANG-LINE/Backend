package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.global.util.CookieUtils
import com.sbl.sulmun2yong.user.service.RefreshTokenService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 게이트웨이 공개 경로(/auth/**). access 만료 시 프론트가 여기로 재발급을 요청한다.
@RestController
@RequestMapping("/auth")
class AuthController(
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private const val REFRESH_TOKEN_COOKIE = "refresh-token"
    }

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val refreshToken = CookieUtils.findCookie(request, REFRESH_TOKEN_COOKIE)?.value
        val newAccessToken =
            refreshTokenService.reissue(refreshToken)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        response.addCookie(jwtTokenProvider.makeAccessTokenCookie(newAccessToken))
        return ResponseEntity.ok().build()
    }
}
