package com.englishvoicecoach.controller;

import com.englishvoicecoach.model.VoiceConversationResponse;
import com.englishvoicecoach.service.VoiceConversationOperations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
public class VoiceConversationController {

    private final VoiceConversationOperations voiceConversationService;

    public VoiceConversationController(VoiceConversationOperations voiceConversationService) {
        this.voiceConversationService = voiceConversationService;
    }

    @PostMapping(value = "/conversation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceConversationResponse> conversation(
            @RequestPart("audio") MultipartFile audio,
            @RequestPart("sessionId") String sessionId) {
        return ResponseEntity.ok(voiceConversationService.converse(audio, sessionId));
    }
}
