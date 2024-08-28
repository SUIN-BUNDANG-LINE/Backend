package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.controller.doc.LoginApiDoc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController("/api/v1/login")
class LoginController : LoginApiDoc {
    @GetMapping("/oauth/{provider}")
    @ResponseBody
    override fun login(
        @PathVariable provider: String,
    ): ResponseEntity<Any> {
        val httpHeaders = HttpHeaders()
        httpHeaders.location = URI.create("http://localhost:8080/oauth2/authorization/$provider")
        return ResponseEntity(httpHeaders, HttpStatus.FOUND)
    }
}
