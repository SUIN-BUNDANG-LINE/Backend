package com.sbl.sulmun2yong.member.controller

import com.sbl.sulmun2yong.global.oauth2.OAuth2Module
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/v1/oauth2")
class Oauth2Controller
    @Autowired
    constructor(
        private var oAuth2Module: OAuth2Module,
    ) {
        @GetMapping("/login/{provider}")
        fun getSocialLoginUrl(
            @PathVariable provider: String,
        ) {
        }

        @GetMapping("/{code}/{provider}")
        fun getSAccessToken(
            @PathVariable code: String,
            @PathVariable provider: String,
        ) {
        }
    }
