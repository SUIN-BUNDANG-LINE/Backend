package com.sbl.sulmun2yong.user.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

// 소셜 로그인 진입 — 프론트가 부르면 OAuth2 인가 엔드포인트로 리다이렉트한다.
// Swagger 문서 인터페이스는 web(springdoc) 소관이라 auth 에서는 두지 않는다.
@RestController
@RequestMapping("/api/v1/login")
class LoginController(
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) {
    @GetMapping("/oauth/{provider}")
    @ResponseBody
    fun login(
        @PathVariable provider: String,
        @RequestParam("redirect_path") redirectPathAfterLogin: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val httpHeaders = HttpHeaders()

        val redirectUriAfterLogin =
            redirectPathAfterLogin?. let {
                URI.create(frontendBaseUrl + it)
            }

        val redirectUriForOAuth2 =
            UriComponentsBuilder
                .fromPath("/oauth2/authorization/{provider}")
                .queryParam("redirect_uri", redirectUriAfterLogin)
                .buildAndExpand(provider)
                .toUriString()

        httpHeaders.location = URI.create(redirectUriForOAuth2)

        return ResponseEntity(httpHeaders, HttpStatus.FOUND)
    }
}
