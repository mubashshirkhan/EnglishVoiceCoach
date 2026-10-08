package com.englishvoicecoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        long connectTimeoutMs,
        long responseTimeoutMs) {
}
