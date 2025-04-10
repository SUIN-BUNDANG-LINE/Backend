package com.sbl.sulmun2yong.global.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException

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

    @ExceptionHandler(AuthenticationException::class)
    protected fun handleAuthenticationException(
        e: AuthenticationException,
        request: HttpServletRequest,
    ): ErrorResponse {
        val errorCode = ErrorCode.LOGIN_REQUIRED
        log.warn("[${request.method}] ${request.requestURI}: ${errorCode.message}")
        return ErrorResponse.of(errorCode)
    }

    @ExceptionHandler(AccessDeniedException::class)
    protected fun handleAccessDeniedException(
        e: AccessDeniedException,
        request: HttpServletRequest,
    ): ErrorResponse {
        val errorCode = ErrorCode.ACCESS_DENIED
        log.warn("[${request.method}] ${request.requestURI}: ${errorCode.message}")
        return ErrorResponse.of(errorCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ErrorResponse {
        log.warn(e.message, e)
        return ErrorResponse.of(ErrorCode.INPUT_INVALID_VALUE, e.bindingResult)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    protected fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ErrorResponse {
        log.warn(e.message, e)
        return ErrorResponse.of(ErrorCode.FILE_SIZE_EXCEEDED)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    protected fun handleNoResourceFoundException(e: NoResourceFoundException): ErrorResponse {
        log.warn(e.message)
        return ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    protected fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ErrorResponse {
        log.warn(e.message)
        return ErrorResponse.of(ErrorCode.NOT_SUPPORTED_METHOD)
    }
}
