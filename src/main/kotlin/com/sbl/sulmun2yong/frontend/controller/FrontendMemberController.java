package com.sbl.sulmun2yong.frontend.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/")
class FrontendMemberController {

    @GetMapping("")
    @ResponseBody
    fun index(): String = "index"

    @GetMapping("/user")
    @ResponseBody
    fun user(): String = "user"

    @GetMapping("/admin")
    @ResponseBody
    fun admin(): String = "admin"

    @GetMapping("/manager")
    @ResponseBody
    fun manager(): String = "manager"

    @GetMapping("/loginForm")
    fun login(): String = "loginForm"

    @GetMapping("/joinForm")
    fun joinForm(): String = "joinForm"

    @GetMapping("/join")
    fun join(): String = "join"
}
