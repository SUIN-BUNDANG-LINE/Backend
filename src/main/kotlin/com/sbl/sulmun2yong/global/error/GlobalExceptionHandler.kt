package com.sbl.sulmun2yong.global.error

import org.slf4j.LoggerFactory
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ErrorResponse {
        log.error(e.message, e)
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(BusinessException::class)
    protected fun handleRuntimeException(e: BusinessException): ErrorResponse {
        log.warn(e.message, e)
        return ErrorResponse.of(e.errorCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ErrorResponse {
        log.warn(e.message, e)
        return ErrorResponse.of(ErrorCode.INPUT_INVALID_VALUE, e.bindingResult)
    }
}
