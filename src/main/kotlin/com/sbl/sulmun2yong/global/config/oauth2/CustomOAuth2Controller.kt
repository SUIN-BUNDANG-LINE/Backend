package com.sbl.sulmun2yong.global.config.oauth2

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1/oauth2")
class CustomOAuth2Controller {
    @GetMapping("/session")
    @ResponseBody
    fun getCurrentSession(): Authentication = SecurityContextHolder.getContext().authentication
}
