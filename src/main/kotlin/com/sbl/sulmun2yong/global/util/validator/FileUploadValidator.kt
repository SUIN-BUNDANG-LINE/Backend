package com.sbl.sulmun2yong.global.util.validator

import com.sbl.sulmun2yong.global.util.exception.FileNameTooLongException
import com.sbl.sulmun2yong.global.util.exception.FileNameTooShortException
import com.sbl.sulmun2yong.global.util.exception.InvalidExtensionException
import com.sbl.sulmun2yong.global.util.exception.NoExtensionExistException
import com.sbl.sulmun2yong.global.util.exception.NoFileExistException
import com.sbl.sulmun2yong.global.util.exception.OutOfFileSizeException
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile

class FileUploadValidator(
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
        ): FileUploadValidator {
            val fileSize = maxFileSize.toBytes()
            val extensions = allowedExtensions.split(",").toMutableList()
            val contentTypes = allowedContentTypes.split(",").toMutableList()

            return FileUploadValidator(
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
