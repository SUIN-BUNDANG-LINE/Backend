package com.sbl.sulmun2yong.global.resolver

import com.sbl.sulmun2yong.global.annotation.LoginUser
import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.global.error.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Component
class LoginUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasLoginUserAnnotation = parameter.getParameterAnnotation(LoginUser::class.java) != null
        println(parameter.parameterType)
        val isUUID = UUID::class.java.isAssignableFrom(parameter.parameterType)
        return hasLoginUserAnnotation && isUUID
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val customOAuth2User = SecurityContextHolder.getContext().authentication.principal
        if (customOAuth2User is CustomOAuth2User) {
            return UUID.fromString(customOAuth2User.name)
        }

        throw AccessDeniedException(ErrorCode.LOGIN_REQUIRED.message)
    }
}
