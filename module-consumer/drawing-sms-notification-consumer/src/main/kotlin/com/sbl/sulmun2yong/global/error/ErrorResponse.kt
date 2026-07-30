package com.sbl.sulmun2yong.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult

class ErrorResponse private constructor(
    httpStatus: HttpStatus,
    body: ErrorResponseBody,
) : ResponseEntity<Any>(body, httpStatus) {
    companion object {
        fun of(code: ErrorCode): ErrorResponse {
            val body =
                ErrorResponseBody(
                    code = code.code,
                    message = code.message,
                )
            return ErrorResponse(code.httpStatus, body)
        }

        fun of(
            code: ErrorCode,
            bindingResult: BindingResult,
        ): ErrorResponse {
            val fieldErrors =
                bindingResult.fieldErrors.map { error ->
                    FieldError(
                        field = error.field,
                        value = error.rejectedValue?.toString() ?: "",
                        reason = error.defaultMessage ?: "",
                    )
                }
            val body =
                ErrorResponseBody(
                    code = code.code,
                    message = code.message,
                    errors = fieldErrors,
                )
            return ErrorResponse(code.httpStatus, body)
        }
    }

    data class ErrorResponseBody(
        val code: String,
        val message: String,
        val errors: List<FieldError> = listOf(),
    )

    data class FieldError(
        val field: String,
        val value: String,
        val reason: String,
    )
}
