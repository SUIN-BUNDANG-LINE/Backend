package com.sbl.sulmun2yong.aws.controller.doc

import com.sbl.sulmun2yong.aws.dto.request.S3UploadRequest
import com.sbl.sulmun2yong.aws.dto.response.S3UploadResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "AWS", description = "AWS 관련 API")
interface S3ApiDoc {
    @Operation(summary = "S3 업로드")
    @PostMapping("/upload")
    fun upload(
        @RequestBody request: S3UploadRequest,
    ): ResponseEntity<S3UploadResponse>
}
