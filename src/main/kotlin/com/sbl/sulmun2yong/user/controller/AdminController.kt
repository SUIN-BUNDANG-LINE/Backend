package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.user.controller.doc.AdminApiDoc
import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import com.sbl.sulmun2yong.user.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.session.SessionRegistry
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/admin")
class AdminController(
    private val sessionRegistry: SessionRegistry,
    private val adminService: AdminService,
) : AdminApiDoc {
    @GetMapping("/sessions/logged-in-users")
    @ResponseBody
    override fun getAllLoggedInUsers(): ResponseEntity<UserSessionsResponse> {
        val allPrincipals = sessionRegistry.allPrincipals
        return ResponseEntity.ok(adminService.getAllLoggedInUsers(allPrincipals))
    }
}
