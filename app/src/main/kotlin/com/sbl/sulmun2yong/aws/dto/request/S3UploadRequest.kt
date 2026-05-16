package com.sbl.sulmun2yong.aws.dto.request

import org.springframework.web.multipart.MultipartFile

class S3UploadRequest(
    val file: MultipartFile,
)
