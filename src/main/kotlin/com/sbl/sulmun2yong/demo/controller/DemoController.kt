package com.sbl.sulmun2yong.demo.controller

import com.sbl.sulmun2yong.demo.controller.doc.DemoApiDoc
import com.sbl.sulmun2yong.demo.dto.request.DemoCreateRequest
import com.sbl.sulmun2yong.demo.dto.response.DemoResponse
import com.sbl.sulmun2yong.demo.service.DemoService
import com.sbl.sulmun2yong.global.response.ApiResponse
import com.sbl.sulmun2yong.global.response.SuccessCode
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/demo")
class DemoController(private val demoService: DemoService) : DemoApiDoc {
    @GetMapping("/{id}")
    override fun getDemo(
        @PathVariable id: Long,
    ): ApiResponse<DemoResponse> {
        val demoResponse = demoService.getDemo(id)
        return ApiResponse.of(SuccessCode.FIND_DEMO_SUCCESS, demoResponse)
    }

    @PostMapping
    override fun createDemo(
        @Valid @RequestBody demoCreateRequest: DemoCreateRequest,
    ): ApiResponse<DemoResponse> {
        val demoResponse = demoService.createDemo(demoCreateRequest)
        return ApiResponse.of(SuccessCode.CREATE_DEMO_SUCCESS, demoResponse)
    }
}
