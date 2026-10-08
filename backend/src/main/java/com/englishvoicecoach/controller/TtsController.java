package com.englishvoicecoach.controller;

import com.englishvoicecoach.model.TtsRequest;
import com.englishvoicecoach.service.TtsService;
import com.englishvoicecoach.service.InvalidTtsRequestException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping(value = "/speak", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "audio/wav")
    public ResponseEntity<byte[]> speak(@RequestBody TtsRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new InvalidTtsRequestException("Text must not be blank.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(ttsService.synthesize(request == null ? null : request.text()));
    }
}
