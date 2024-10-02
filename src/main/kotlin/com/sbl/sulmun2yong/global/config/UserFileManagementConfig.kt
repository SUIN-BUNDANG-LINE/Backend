package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.util.validator.FileUploadValidator
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class UserFileManagementConfig(
    // S3 클라이언트 관련
    @Value("\${aws.s3.access-key}")
    private val accessKey: String,
    @Value("\${aws.s3.secret-key}")
    private val secretKey: String,
    // 파일 예외처리 관련
    @Value("\${aws.s3.max-file-name-length}")
    private val maxFileNameLength: Int,
    @Value("\${aws.s3.allowed-extensions}")
    private val allowedExtensions: String,
    @Value("\${aws.s3.allowed-content-types}")
    private val allowedContentTypes: String,
    @Value("\${cloudfront.base-url}")
    private val cloudFrontBaseUrl: String,
) {
    @Bean
    fun createS3Client(): S3Client {
        val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build()
    }

    @Bean
    fun createFileUploadValidator(): FileUploadValidator =
        FileUploadValidator.from(
            maxFileNameLength = maxFileNameLength,
            allowedExtensions = allowedExtensions,
            allowedContentTypes = allowedContentTypes,
        )

    @Bean
    fun createFileUrlValidator(): FileUrlValidator = FileUrlValidator.of(cloudFrontBaseUrl)
}
