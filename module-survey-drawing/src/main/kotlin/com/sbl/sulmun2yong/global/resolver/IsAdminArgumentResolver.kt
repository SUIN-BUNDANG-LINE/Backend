package com.sbl.sulmun2yong.global.resolver

import com.sbl.sulmun2yong.global.annotation.IsAdmin
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class IsAdminArgumentResolver : HandlerMethodArgumentResolver {
    companion object {
        // 게이트웨이가 주입하는 X-User-Role 헤더 값(auth 의 UserRole 과 wire 로 만나는 계약 문자열)
        private const val ROLE_ADMIN = "ROLE_ADMIN"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasIsAdminAnnotation = parameter.getParameterAnnotation(IsAdmin::class.java) != null
        val isBoolean = Boolean::class.java.isAssignableFrom(parameter.parameterType)
        return hasIsAdminAnnotation && isBoolean
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any = webRequest.getHeader("X-User-Role") == ROLE_ADMIN
}
