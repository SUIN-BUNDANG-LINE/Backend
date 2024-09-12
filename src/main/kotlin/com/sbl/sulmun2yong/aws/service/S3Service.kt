package com.sbl.sulmun2yong.aws.service

import com.sbl.sulmun2yong.aws.dto.response.S3UploadResponse
import com.sbl.sulmun2yong.aws.exception.FileNameTooLongException
import com.sbl.sulmun2yong.aws.exception.FileNameTooShortException
import com.sbl.sulmun2yong.aws.exception.InvalidExtensionException
import com.sbl.sulmun2yong.aws.exception.NoFileExistException
import com.sbl.sulmun2yong.aws.exception.OutOfFileSizeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class S3Service(
    @Value("\${aws.s3.bucket-name}")
    private val bucketName: String,
    @Value("\${cloudfront.base-url}")
    private val cloudFrontUrl: String,
    private val s3Client: S3Client,
) {
    fun uploadFile(receivedFile: MultipartFile): S3UploadResponse {
        validateReceivedFile(receivedFile)

        val tempFilePath: Path = Files.createTempFile(receivedFile.originalFilename, null)
        receivedFile.transferTo(tempFilePath)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val keyName = "${receivedFile.originalFilename}_$timestamp"

        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(keyName)
                .build()

        s3Client.putObject(putObjectRequest, tempFilePath)
        Files.deleteIfExists(tempFilePath)

        return S3UploadResponse("$cloudFrontUrl/$keyName")
    }
}

private fun validateReceivedFile(receivedFile: MultipartFile) {
    val maxFileSize = 5 * 1024 * 1024
    val maxFileLength = 100
    val allowedExtension = mutableListOf("application/pdf", "image/jpeg", "image/png")

    val fileSize = receivedFile.size
    val fileName: String? = receivedFile.originalFilename
    val contentType = receivedFile.contentType

    if (receivedFile.isEmpty) {
        throw NoFileExistException()
    }
    if (fileSize > maxFileSize) {
        throw OutOfFileSizeException()
    }
    if (fileName.isNullOrBlank()) {
        throw FileNameTooShortException()
    }
    if (fileName.length > maxFileLength) {
        throw FileNameTooLongException()
    }
    if (!allowedExtension.contains(contentType)) {
        throw InvalidExtensionException()
    }
}
