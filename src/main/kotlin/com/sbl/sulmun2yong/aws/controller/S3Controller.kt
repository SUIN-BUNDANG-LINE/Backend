
import com.sbl.sulmun2yong.aws.controller.doc.S3ApiDoc
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@RestController
@RequestMapping("/api/v1/s3")
class S3Controller(
    @Value("\${aws.s3.bucket-name}")
    private val bucketName: String,
    private val s3Service: S3Service,
) : S3ApiDoc {
    @PostMapping("/upload")
    override fun uploadFile(
        @RequestPart(value = "file") file: MultipartFile,
    ): ResponseEntity<String> {
        val tempFile: Path = Files.createTempFile(file.originalFilename, null)
        file.transferTo(tempFile)

        val bucketName = bucketName
        val keyName = file.originalFilename ?: "default-name"

        s3Service.uploadFile(bucketName, keyName, tempFile.toFile())

        // 임시 파일 삭제
        Files.delete(tempFile)

        return ResponseEntity.ok("File uploaded successfully")
    }
}
