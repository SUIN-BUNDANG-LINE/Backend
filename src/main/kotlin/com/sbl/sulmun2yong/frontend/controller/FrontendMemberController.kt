package com.sbl.sulmun2yong.frontend.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/frontend")
class FrontendMemberController {
    @GetMapping("", "/")
    fun index(): String = "index"

    @GetMapping("/user")
    fun user(): String = "user"

    @GetMapping("/admin")
    fun admin(): String = "admin"

    @GetMapping("/loginForm")
    fun login(): String = "loginForm"

    @GetMapping("/joinForm")
    fun joinForm(): String = "joinForm"
}
