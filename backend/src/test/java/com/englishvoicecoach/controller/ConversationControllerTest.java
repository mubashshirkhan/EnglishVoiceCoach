package com.englishvoicecoach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishvoicecoach.model.ConversationResponse;
import com.englishvoicecoach.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;

class ConversationControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ConversationService conversationService = message ->
                new ConversationResponse(message, message, true, java.util.List.of(), null, "Hi!", false);
        mockMvc = MockMvcBuilders.standaloneSetup(new ConversationController(conversationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsStructuredConversationResponse() throws Exception {
        mockMvc.perform(post("/api/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"originalMessage":"Hello","grammarCorrect":true,"errors":[],"conversationResponse":"Hi!","needsCorrectionPractice":false}
                        """));
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/conversation/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"error":"INVALID_MESSAGE","message":"Message must not be blank."}
                        """));
    }
}
