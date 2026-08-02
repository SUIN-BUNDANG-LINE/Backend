package com.sbl.sulmun2yong.global.resolver

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.global.error.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.nio.file.AccessDeniedException
import java.util.*

@Component
class LoginUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasLoginUserAnnotation = parameter.getParameterAnnotation(LoginUser::class.java) != null
        val isUUID = UUID::class.java.isAssignableFrom(parameter.parameterType)
        return hasLoginUserAnnotation && isUUID
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UUID {
        val userId =
            webRequest.getHeader("X-User-Id")
                ?: throw AccessDeniedException(ErrorCode.LOGIN_REQUIRED.message)
        return UUID.fromString(userId)
    }
}
