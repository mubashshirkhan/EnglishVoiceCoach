package com.englishvoicecoach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishvoicecoach.service.TtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TtsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TtsService service = text -> new byte[] {'R', 'I', 'F', 'F', 0};
        mockMvc = MockMvcBuilders.standaloneSetup(new TtsController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsWavAudio() throws Exception {
        mockMvc.perform(post("/api/tts/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(content().bytes(new byte[] {'R', 'I', 'F', 'F', 0}));
    }

    @Test
    void rejectsEmptyText() throws Exception {
        mockMvc.perform(post("/api/tts/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INVALID_TTS_REQUEST\"}"));
    }
}
