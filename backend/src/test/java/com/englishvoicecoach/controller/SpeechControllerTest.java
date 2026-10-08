package com.englishvoicecoach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishvoicecoach.model.TranscriptionResponse;
import com.englishvoicecoach.service.SpeechToTextService;
import com.englishvoicecoach.service.SttResponseException;
import com.englishvoicecoach.service.SttUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpeechControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SpeechToTextService service = audio -> new TranscriptionResponse(
                "Hello, how are you?", "en", 2.4, 1.1);
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeechController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsValidTranscription() throws Exception {
        mockMvc.perform(multipart("/api/transcribe")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1, 2})))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"text":"Hello, how are you?","language":"en","duration":2.4,"transcriptionDuration":1.1}
                        """));
    }

    @Test
    void rejectsMissingAudio() throws Exception {
        mockMvc.perform(multipart("/api/transcribe"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsInvalidResponseError() throws Exception {
        SpeechToTextService service = audio -> {
            throw new SttResponseException("invalid");
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeechController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(multipart("/api/transcribe")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1})))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"error":"INVALID_STT_RESPONSE"}
                        """));
    }

    @Test
    void returnsUnavailableError() throws Exception {
        SpeechToTextService service = audio -> {
            throw new SttUnavailableException("offline", new RuntimeException());
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeechController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(multipart("/api/transcribe")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"error":"STT_UNAVAILABLE"}
                        """));
    }

    @Test
    void returnsTimeoutAsUnavailableError() throws Exception {
        SpeechToTextService service = audio -> {
            throw new SttUnavailableException("timeout", new java.net.SocketTimeoutException());
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeechController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(multipart("/api/transcribe")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"error":"STT_UNAVAILABLE"}
                        """));
    }
}
