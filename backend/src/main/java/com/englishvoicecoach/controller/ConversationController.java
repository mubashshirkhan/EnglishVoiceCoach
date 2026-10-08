package com.englishvoicecoach.controller;

import com.englishvoicecoach.model.ConversationMessageRequest;
import com.englishvoicecoach.model.ConversationResponse;
import com.englishvoicecoach.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/message")
    public ResponseEntity<ConversationResponse> message(@RequestBody ConversationMessageRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new InvalidMessageException("Message must not be blank.");
        }
        return ResponseEntity.ok(conversationService.respond(request.message().trim()));
    }
}
