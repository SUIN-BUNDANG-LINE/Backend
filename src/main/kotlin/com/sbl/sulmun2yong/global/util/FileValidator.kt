package com.sbl.sulmun2yong.global.util

import com.sbl.sulmun2yong.aws.exception.FileNameTooLongException
import com.sbl.sulmun2yong.aws.exception.FileNameTooShortException
import com.sbl.sulmun2yong.aws.exception.InvalidExtensionException
import com.sbl.sulmun2yong.aws.exception.NoExtensionExistException
import com.sbl.sulmun2yong.aws.exception.NoFileExistException
import com.sbl.sulmun2yong.aws.exception.OutOfFileSizeException
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile

class FileValidator(
    private val maxFileSize: Long,
    private val maxFileNameLength: Int,
    private val allowedExtensions: MutableList<String>,
    private val allowedContentTypes: MutableList<String>,
) {
    companion object {
        fun from(
            maxFileSize: DataSize,
            maxFileNameLength: Int,
            allowedExtensions: String,
            allowedContentTypes: String,
        ): FileValidator {
            val fileSize = maxFileSize.toBytes()
            val extensions = allowedExtensions.split(",").toMutableList()
            val contentTypes = allowedContentTypes.split(",").toMutableList()

            return FileValidator(
                fileSize,
                maxFileNameLength,
                extensions,
                contentTypes,
            )
        }
    }

    fun validateFileOf(file: MultipartFile) {
        fun checkIsAllowedExtensionOrType(contentType: String): Boolean =
            allowedExtensions.contains(contentType) || allowedContentTypes.any { contentType.startsWith(it) }

        val fileSize = file.size
        val fileName: String? = file.originalFilename
        val contentType = file.contentType

        if (file.isEmpty) {
            throw NoFileExistException()
        }
        if (contentType.isNullOrBlank()) {
            throw NoExtensionExistException()
        }
        if (fileSize > maxFileSize) {
            throw OutOfFileSizeException()
        }
        if (fileName.isNullOrBlank()) {
            throw FileNameTooShortException()
        }
        if (fileName.length > maxFileNameLength) {
            throw FileNameTooLongException()
        }
        if (!checkIsAllowedExtensionOrType(contentType)) {
            throw InvalidExtensionException()
        }
    }
}
