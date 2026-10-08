package com.englishvoicecoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.englishvoicecoach.config.OllamaProperties;
import com.englishvoicecoach.config.SttProperties;
import com.englishvoicecoach.config.TtsProperties;

@SpringBootApplication
@EnableConfigurationProperties({OllamaProperties.class, SttProperties.class, TtsProperties.class})
public class EnglishVoiceCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishVoiceCoachApplication.class, args);
    }
}
