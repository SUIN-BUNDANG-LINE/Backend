package com.sbl.sulmun2yong.global.config

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebDriverConfig {
    @Bean
    fun webDriver(): WebDriver {
        // ChromeDriver 설정
        WebDriverManager.chromedriver().setup()

        // ChromeOptions 설정
        val options = ChromeOptions()
        options.addArguments("--headless")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        // WebDriver 생성 및 반환
        return ChromeDriver(options)
    }
}
