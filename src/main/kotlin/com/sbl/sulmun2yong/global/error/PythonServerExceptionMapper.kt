package com.sbl.sulmun2yong.global.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.ai.exception.FileExtensionNotSupportedException
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.ai.exception.TextTooLongException
import org.springframework.web.client.HttpClientErrorException

class PythonServerExceptionMapper {
    companion object {
        private val objectMapper = ObjectMapper()

        data class ErrorDetail(
            val code: String = "",
            val message: String = "",
        )

        data class PythonServerException(
            val detail: ErrorDetail = ErrorDetail(),
        )

        fun mapException(e: HttpClientErrorException): BusinessException {
            val exception = objectMapper.readValue(e.responseBodyAsString, PythonServerException::class.java)
            when (exception.detail.code) {
                "PY0001" -> throw SurveyGenerationByAIFailedException()
                "PY0002" -> throw TextTooLongException()
                "PY0003" -> throw FileExtensionNotSupportedException()
                else -> throw e
            }
        }
    }
}
