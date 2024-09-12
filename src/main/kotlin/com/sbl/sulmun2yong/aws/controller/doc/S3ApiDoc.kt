package com.sbl.sulmun2yong.aws.controller.doc

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@Tag(name = "AWS", description = "AWS 관련 API")
interface S3ApiDoc {
    @Operation(summary = "S3 업로드")
    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<String>
}
