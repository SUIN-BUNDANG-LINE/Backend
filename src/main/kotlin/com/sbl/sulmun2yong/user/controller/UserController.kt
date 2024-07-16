package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
)
