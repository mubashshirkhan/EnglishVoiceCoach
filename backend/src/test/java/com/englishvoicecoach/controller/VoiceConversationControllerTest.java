package com.englishvoicecoach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishvoicecoach.model.VoiceConversationResponse;
import com.englishvoicecoach.model.VoiceSessionState;
import com.englishvoicecoach.service.VoiceConversationOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VoiceConversationControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VoiceConversationOperations service = (audio, sessionId) -> new VoiceConversationResponse(
                sessionId, "Hello", VoiceSessionState.NORMAL_CONVERSATION, null, null, "Hi!", false);
        mockMvc = MockMvcBuilders.standaloneSetup(new VoiceConversationController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void acceptsAudioAndSessionId() throws Exception {
        mockMvc.perform(multipart("/api/voice/conversation")
                        .file(new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1}))
                        .file(new MockMultipartFile("sessionId", "", MediaType.TEXT_PLAIN_VALUE, "session-1".getBytes())))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"sessionId":"session-1","transcript":"Hello","state":"NORMAL_CONVERSATION",
                         "conversationResponse":"Hi!","needsCorrectionPractice":false}
                        """));
    }
}
