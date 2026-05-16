package com.sbl.sulmun2yong.aws.controller

import com.sbl.sulmun2yong.aws.controller.doc.S3ApiDoc
import com.sbl.sulmun2yong.aws.dto.request.S3UploadRequest
import com.sbl.sulmun2yong.aws.dto.response.S3UploadResponse
import com.sbl.sulmun2yong.aws.service.S3Service
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/s3")
class S3Controller(
    private val s3Service: S3Service,
) : S3ApiDoc {
    @PostMapping("/upload")
    override fun upload(
        @ModelAttribute request: S3UploadRequest,
    ): ResponseEntity<S3UploadResponse> = ResponseEntity.ok(s3Service.uploadFile(request.file))
}
