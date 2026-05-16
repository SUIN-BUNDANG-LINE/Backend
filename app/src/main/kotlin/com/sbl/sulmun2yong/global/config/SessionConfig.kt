package com.sbl.sulmun2yong.global.config

import jakarta.servlet.ServletContext
import jakarta.servlet.SessionCookieConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SessionConfig(
    @Value("\${cookie.domain}")
    private val cookieDomain: String,
) {
    @Bean
    fun servletContextInitializer(): ServletContextInitializer =
        ServletContextInitializer { servletContext: ServletContext ->
            val sessionCookieConfig: SessionCookieConfig = servletContext.sessionCookieConfig
            if (cookieDomain != "localhost") {
                sessionCookieConfig.domain = cookieDomain
            }
        }
}
