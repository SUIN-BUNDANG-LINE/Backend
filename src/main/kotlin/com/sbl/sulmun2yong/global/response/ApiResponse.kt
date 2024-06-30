package com.sbl.sulmun2yong.global.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ApiResponse<T> private constructor(
    httpStatus: HttpStatus,
    body: ApiResponseBody<T>,
) : ResponseEntity<Any>(body, httpStatus) {
    companion object {
        fun <T> of(
            successCode: SuccessCode,
            data: T,
        ): ApiResponse<T> {
            val body = ApiResponseBody(successCode.code, successCode.message, data)
            return ApiResponse(successCode.httpStatus, body)
        }
    }

    data class ApiResponseBody<T>(
        val code: String,
        val message: String,
        val data: T?,
    )
}
