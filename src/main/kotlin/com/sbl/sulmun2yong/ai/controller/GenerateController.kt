package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.GenerateAPIDoc
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerateRequest
import com.sbl.sulmun2yong.ai.service.GenerateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ai/generate")
class GenerateController(
    private val generateService: GenerateService,
) : GenerateAPIDoc {
    @PostMapping("/survey")
    override fun generateSurvey(
        @RequestBody request: SurveyGenerateRequest,
    ) = ResponseEntity.ok(generateService.generateSurvey(request.job, request.groupName, request.fileUrl))
}
