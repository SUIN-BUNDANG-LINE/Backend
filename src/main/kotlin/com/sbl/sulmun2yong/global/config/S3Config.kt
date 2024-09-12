import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config(
    @Value("\${aws.s3.access-key}")
    private val accessKey: String,
    @Value("\${aws.s3.secret-key}")
    private val secretKey: String,
) {
    @Bean
    fun s3Client(): S3Client {
        // 액세스 키와 시크릿 키 설정
        val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build()
    }
}
