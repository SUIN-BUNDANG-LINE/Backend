package com.sbl.sulmun2yong.ai.controller

import com.sbl.sulmun2yong.ai.controller.doc.GenerateAPIDoc
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
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
    @PostMapping("/survey/file-url")
    override fun generateSurveyWithFileUrl(
        @RequestBody request: SurveyGenerationWithFileUrlRequest,
    ) = ResponseEntity.ok(generateService.generateSurveyWithFileUrl(request.job, request.groupName, request.fileUrl, request.userPrompt))

    @PostMapping("/survey/text-document")
    override fun generateSurveyWithTextDocument(
        @RequestBody request: SurveyGenerationWithTextDocumentRequest,
    ) = ResponseEntity.ok(
        generateService.generateSurveyWithTextDocument(request.job, request.groupName, request.textDocument, request.userPrompt),
    )
}
