package com.sbl.sulmun2yong.member.config

import org.springframework.boot.web.servlet.view.MustacheViewResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        val resolver = MustacheViewResolver()
        resolver.setCharset("UTF-8")
        resolver.setContentType("text/html; charset=UTF-8")
        resolver.setPrefix("classpath:/templates/")
        resolver.setSuffix(".html")

        registry.viewResolver(resolver)
    }
}
