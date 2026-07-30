package com.sbl.sulmun2yong.global.config

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress

@Configuration
class MetricsConfig {
    /**
     * 모든 메트릭에 instance(hostname) 공통 태그를 부여한다.
     * 컨테이너 이름(sulmun2yong-web-1 등)이 hostname으로 들어와
     * Grafana에서 web-1/web-2/consumer-1~3을 라벨로 구분 가능.
     */
    @Bean
    fun commonInstanceTag(): MeterRegistryCustomizer<MeterRegistry> =
        MeterRegistryCustomizer { registry ->
            registry.config().commonTags("instance", resolveInstance())
        }

    private fun resolveInstance(): String =
        runCatching {
            InetAddress.getLocalHost().hostName
        }.getOrDefault("unknown")
}
