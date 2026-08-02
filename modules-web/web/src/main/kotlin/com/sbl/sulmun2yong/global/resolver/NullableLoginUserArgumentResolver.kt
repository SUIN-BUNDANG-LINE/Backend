package com.sbl.sulmun2yong.global.resolver

import com.sbl.sulmun2yong.global.annotation.NullableLoginUser
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.*

@Component
class NullableLoginUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasNullableLoginUserAnnotation =
            parameter.getParameterAnnotation(NullableLoginUser::class.java) != null
        val isUUID = parameter.parameterType == UUID::class.javaObjectType
        return hasNullableLoginUserAnnotation && isUUID
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UUID? {
        val userId = webRequest.getHeader("X-User-Id") ?: return null
        return UUID.fromString(userId)
    }
}
