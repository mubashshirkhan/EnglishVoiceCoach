package com.englishvoicecoach.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

class LocalSpeechToTextServiceTest {

    private LocalSpeechToTextService service;

    @BeforeEach
    void setUp() {
        service = new LocalSpeechToTextService(RestClient.builder().baseUrl("http://127.0.0.1:1").build());
    }

    @Test
    void rejectsMissingAudio() {
        assertThatThrownBy(() -> service.transcribe(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyAudio() {
        MockMultipartFile audio = new MockMultipartFile("audio", "empty.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() -> service.transcribe(audio))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedAudio() {
        MockMultipartFile audio = new MockMultipartFile("audio", "notes.txt", "text/plain", new byte[] {1});

        assertThatThrownBy(() -> service.transcribe(audio))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsWebmCodecParameterBeforeForwarding() {
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "speech.webm", "audio/webm;codecs=opus", new byte[] {1});

        assertThatThrownBy(() -> service.transcribe(audio))
                .isInstanceOf(SttUnavailableException.class);
    }
}
