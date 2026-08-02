package com.sbl.sulmun2yong.global.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @Value("\${spring.data.redis.host}")
    private val redisHost: String,
    @Value("\${spring.data.redis.port}")
    private val redisPort: Int,
    @Value("\${spring.data.redis.password:}")
    private val redisPassword: String,
) {
    @Bean(destroyMethod = "shutdown")
    fun redissonClient(): RedissonClient {
        val config = Config()
        val redisAddress = "redis://$redisHost:$redisPort"
        config.useSingleServer().setAddress(redisAddress)
        if (redisPassword.isNotEmpty()) config.useSingleServer().setPassword(redisPassword)
        return Redisson.create(config)
    }
}
