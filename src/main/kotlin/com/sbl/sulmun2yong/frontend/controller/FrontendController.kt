package com.sbl.sulmun2yong.frontend.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/frontend")
class FrontendController {
    @GetMapping("/login")
    fun login(): String = "login"

    @GetMapping("/invalid-session")
    fun invalidSession(): String = "invalid-session"

    @GetMapping("/expired")
    fun expired(): String = "expired"
}
