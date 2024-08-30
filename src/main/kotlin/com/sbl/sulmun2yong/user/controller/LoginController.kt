package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.controller.doc.LoginApiDoc
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.QueryParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController("/api/v1/login")
class LoginController(
    @Value("\${backend.base-url}")
    private val backendBaseUrl: String,
) : LoginApiDoc {
    @GetMapping("/oauth/{provider}")
    @ResponseBody
    override fun login(
        @PathVariable provider: String,
        @QueryParam("redirect_uri") redirectUrl: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val userRedirectUrl = redirectUrl ?: backendBaseUrl
        val httpHeaders = HttpHeaders()

        val oauth2RedirectUrl = "/oauth2/authorization/$provider?redirect_uri=$userRedirectUrl"
        httpHeaders.location = URI.create(oauth2RedirectUrl)
        return ResponseEntity(httpHeaders, HttpStatus.FOUND)
    }
}
