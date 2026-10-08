package com.englishvoicecoach.controller;

import com.englishvoicecoach.model.TranscriptionResponse;
import com.englishvoicecoach.service.SpeechToTextService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class SpeechController {

    private final SpeechToTextService speechToTextService;

    public SpeechController(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(@RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.ok(speechToTextService.transcribe(audio));
    }
}
