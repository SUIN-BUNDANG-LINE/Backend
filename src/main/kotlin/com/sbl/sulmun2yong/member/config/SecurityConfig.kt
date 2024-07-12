package com.sbl.sulmun2yong.member.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity // 스프링 시큐리티 필터가 스프링 필터 체인에 등록이 됩니다
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf {
            it.disable()
        }
        http.authorizeHttpRequests {
            it.requestMatchers("/user/**").authenticated()
            it.requestMatchers("/manager/**").hasRole("MANAGER or ADMIN")
            it.requestMatchers("/admin/**").hasRole("ADMIN")
            it.anyRequest().permitAll()
        }
        http.formLogin {
            it.loginPage("/loginForm")
        }
        return http.build()
    }
}
