package com.sbl.sulmun2yong.global.resolver

import com.sbl.sulmun2yong.global.annotation.NullableLoginUser
import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Component
class NullableLoginUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasNullableLoginUserAnnotation = parameter.getParameterAnnotation(NullableLoginUser::class.java) != null
        val isUUID = parameter.parameterType == UUID::class.javaObjectType
        return hasNullableLoginUserAnnotation && isUUID
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val authentication = SecurityContextHolder.getContext().authentication
        val customOAuth2User = authentication?.principal

        if (customOAuth2User is CustomOAuth2User) {
            return UUID.fromString(customOAuth2User.name)
        }

        return null
    }
}
