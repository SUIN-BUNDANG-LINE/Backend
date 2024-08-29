package com.sbl.sulmun2yong.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.SerializationUtils
import java.util.Base64
import java.util.Optional

object CookieUtils {
    fun getCookie(
        request: HttpServletRequest,
        name: String?,
    ): Optional<Cookie> {
        val cookies: Array<Cookie>? = request.cookies

        if (cookies != null && cookies.size > 0) {
            for (cookie in cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of<Cookie>(cookie)
                }
            }
        }

        return Optional.empty<Cookie>()
    }

    fun addCookie(
        response: HttpServletResponse,
        name: String?,
        value: String?,
        maxAge: Int,
    ) {
        val cookie: Cookie = Cookie(name, value)
        cookie.setPath("/")
        cookie.setHttpOnly(true)
        cookie.setMaxAge(maxAge)
        response.addCookie(cookie)
    }

    fun deleteCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
        name: String?,
    ) {
        val cookies: Array<Cookie>? = request.cookies
        if (cookies != null && cookies.size > 0) {
            for (cookie in cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("")
                    cookie.setPath("/")
                    cookie.setMaxAge(0)
                    response.addCookie(cookie)
                }
            }
        }
    }

    fun serialize(`object`: Any?): String =
        Base64
            .getUrlEncoder()
            .encodeToString(SerializationUtils.serialize(`object`))

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
