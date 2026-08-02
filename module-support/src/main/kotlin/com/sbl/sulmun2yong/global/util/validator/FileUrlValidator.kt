package com.sbl.sulmun2yong.global.util.validator

import com.sbl.sulmun2yong.global.util.exception.InvalidExtensionException
import com.sbl.sulmun2yong.global.util.exception.InvalidFileUrlException

class FileUrlValidator(
    private val cloudFrontBaseUrl: String,
) {
    companion object {
        fun of(cloudFrontBaseUrl: String): FileUrlValidator = FileUrlValidator(cloudFrontBaseUrl)
    }

    fun validateFileUrlOf(
        fileUrl: String,
        allowedExtensions: MutableList<String>,
    ) {
        if (fileUrl.startsWith(cloudFrontBaseUrl).not()) {
            throw InvalidFileUrlException()
        }
        if (allowedExtensions.none { fileUrl.endsWith(it) }) {
            throw InvalidExtensionException()
        }
    }
}
