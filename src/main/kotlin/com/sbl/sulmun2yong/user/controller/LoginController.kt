package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.controller.doc.LoginApiDoc
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

@RestController
@RequestMapping("/api/v1/login")
class LoginController(
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
) : LoginApiDoc {
    @GetMapping("/oauth/{provider}")
    @ResponseBody
    override fun login(
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
