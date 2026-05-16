package com.sbl.sulmun2yong.global.util.validator

import com.sbl.sulmun2yong.global.util.exception.FileNameTooLongException
import com.sbl.sulmun2yong.global.util.exception.FileNameTooShortException
import com.sbl.sulmun2yong.global.util.exception.InvalidExtensionException
import com.sbl.sulmun2yong.global.util.exception.NoExtensionExistException
import com.sbl.sulmun2yong.global.util.exception.NoFileExistException
import org.springframework.web.multipart.MultipartFile

class FileUploadValidator(
    private val maxFileNameLength: Int,
    private val allowedExtensions: MutableList<String>,
    private val allowedContentTypes: MutableList<String>,
) {
    companion object {
        fun from(
            maxFileNameLength: Int,
            allowedExtensions: String,
            allowedContentTypes: String,
        ): FileUploadValidator {
            val extensions = allowedExtensions.split(",").toMutableList()
            val contentTypes = allowedContentTypes.split(",").toMutableList()

            return FileUploadValidator(
                maxFileNameLength,
                extensions,
                contentTypes,
            )
        }
    }

    fun validateFileOf(file: MultipartFile) {
        fun checkIsAllowedExtensionOrType(contentType: String): Boolean =
            allowedExtensions.contains(contentType) || allowedContentTypes.any { contentType.startsWith(it) }

        val fileName: String? = file.originalFilename
        val contentType = file.contentType

        if (file.isEmpty) {
            throw NoFileExistException()
        }
        if (contentType.isNullOrBlank()) {
            throw NoExtensionExistException()
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
