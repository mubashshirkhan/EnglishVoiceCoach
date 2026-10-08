package com.englishvoicecoach.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({OllamaProperties.class, SttProperties.class})
public class HttpClientConfig {

    @Bean
    RestClient ollamaRestClient(RestClient.Builder builder, OllamaProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties.connectTimeoutMs(), properties.responseTimeoutMs()))
                .build();
    }

    @Bean
    RestClient sttRestClient(RestClient.Builder builder, SttProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties.connectTimeoutMs(), properties.responseTimeoutMs()))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(long connectTimeoutMs, long responseTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(responseTimeoutMs));
        return requestFactory;
    }
}
