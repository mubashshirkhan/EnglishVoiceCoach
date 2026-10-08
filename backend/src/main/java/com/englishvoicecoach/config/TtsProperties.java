package com.englishvoicecoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tts")
public record TtsProperties(
        String executable,
        String model,
        long timeoutMs) {
}
