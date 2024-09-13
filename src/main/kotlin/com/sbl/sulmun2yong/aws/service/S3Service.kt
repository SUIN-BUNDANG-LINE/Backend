package com.sbl.sulmun2yong.aws.service

import com.sbl.sulmun2yong.aws.dto.response.S3UploadResponse
import com.sbl.sulmun2yong.global.util.FileValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class S3Service(
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket-name}")
    private val bucketName: String,
    @Value("\${cloudfront.base-url}")
    private val cloudFrontUrl: String,
    private val fileValidator: FileValidator,
) {
    fun uploadFile(receivedFile: MultipartFile): S3UploadResponse {
        fileValidator.validateFileOf(receivedFile)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val keyName = "${timestamp}_${receivedFile.originalFilename}"

        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(keyName)
                .build()

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(receivedFile.inputStream, receivedFile.size))

        return S3UploadResponse("$cloudFrontUrl/$keyName")
    }
}
