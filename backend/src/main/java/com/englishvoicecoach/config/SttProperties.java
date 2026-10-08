package com.englishvoicecoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stt")
public record SttProperties(
        String baseUrl,
        long connectTimeoutMs,
        long responseTimeoutMs) {
}
