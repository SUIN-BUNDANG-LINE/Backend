package com.sbl.sulmun2yong.global.error

import jakarta.servlet.http.HttpServletRequest
import org.hibernate.exception.LockAcquisitionException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
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

    // 계측은 DrawingProcessMetrics(mode 라벨 포함)가 전담한다. 여기서는 진단용 로그만 남긴다.
    // 지연 로딩 조회에서 난 데드락은 Spring 예외 변환을 타지 않아 Hibernate 고유 예외로 올라오므로
    // 두 타입을 함께 받는다 — 그러지 않으면 같은 데드락이 일반 오류 로그에 섞인다.
    @ExceptionHandler(CannotAcquireLockException::class, LockAcquisitionException::class)
    protected fun handleDeadlock(e: Exception): ErrorResponse {
        log.error("DB deadlock 발생: ${e.message}", e)
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    // 보드 버전 충돌과 경합 중 사라진 행이 같은 타입으로 올라온다 — 메시지의 엔티티 이름으로 구분한다
    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    protected fun handleOptimisticLockConflict(e: ObjectOptimisticLockingFailureException): ErrorResponse {
        log.error("동시 수정 충돌 발생: ${e.message}")
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
