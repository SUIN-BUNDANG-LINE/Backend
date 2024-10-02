package com.sbl.sulmun2yong.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.SerializationUtils
import java.util.Base64

object CookieUtils {
    fun getCookie(
        request: HttpServletRequest,
        name: String,
    ): Cookie {
        val cookies: Array<Cookie> = request.cookies

        return cookies.firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("존재하지 않는 쿠키입니다")
    }

    fun findCookie(
        request: HttpServletRequest,
        name: String,
    ): Cookie? {
        val cookies: Array<Cookie> = request.cookies
        return cookies.firstOrNull { it.name == name }
    }

    fun addCookie(
        response: HttpServletResponse,
        name: String,
        value: String,
        maxAge: Int,
    ) {
        val cookie = Cookie(name, value)
        cookie.path = "/"
        cookie.isHttpOnly = true
        cookie.maxAge = maxAge
        response.addCookie(cookie)
    }

    fun deleteCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
        name: String,
    ) {
        val cookies: Array<Cookie>? = request.cookies
        if (cookies.isNullOrEmpty()) {
            return
        }

        for (cookie in cookies) {
            if (cookie.name == name) {
                cookie.value = ""
                cookie.path = "/"
                cookie.maxAge = 0
                response.addCookie(cookie)
            }
        }
    }

    fun serialize(cookieObject: Any): String =
        Base64
            .getUrlEncoder()
            .encodeToString(SerializationUtils.serialize(cookieObject))

    fun <T> deserialize(
        cookie: Cookie,
        cls: Class<T>,
    ): T =
        cls.cast(
            SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue()),
            ),
        )
}
