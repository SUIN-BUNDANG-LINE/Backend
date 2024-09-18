package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.util.FileValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config(
    // S3 클라이언트 관련
    @Value("\${aws.s3.access-key}")
    private val accessKey: String,
    @Value("\${aws.s3.secret-key}")
    private val secretKey: String,
    // 파일 예외처리 관련
    @Value("\${aws.s3.max-file-size}")
    private val maxFileSize: DataSize,
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
    fun s3Client(): S3Client {
        val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build()
    }

    @Bean
    fun fileValidateValues(): FileValidator =
        FileValidator.from(
            maxFileSize = maxFileSize,
            maxFileNameLength = maxFileNameLength,
            allowedExtensions = allowedExtensions,
            allowedContentTypes = allowedContentTypes,
            cloudFrontBaseUrl = cloudFrontBaseUrl,
        )
}
