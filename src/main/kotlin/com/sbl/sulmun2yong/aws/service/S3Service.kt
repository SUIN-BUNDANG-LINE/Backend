import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.file.Paths

@Service
class S3Service(
    private val s3Client: S3Client,
) {
    fun uploadFile(
        bucketName: String,
        keyName: String,
        file: File,
    ) {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(keyName)
                .build()

        s3Client.putObject(putObjectRequest, Paths.get(file.toURI()))
    }
}
