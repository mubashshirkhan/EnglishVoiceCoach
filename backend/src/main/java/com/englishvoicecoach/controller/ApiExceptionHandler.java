package com.englishvoicecoach.controller;

import java.util.Map;

import com.englishvoicecoach.service.LocalAiUnavailableException;
import com.englishvoicecoach.service.ModelResponseException;
import com.englishvoicecoach.service.SttResponseException;
import com.englishvoicecoach.service.SttUnavailableException;
import com.englishvoicecoach.service.InvalidVoiceRequestException;
import com.englishvoicecoach.service.PiperProcessException;
import com.englishvoicecoach.service.PiperTimeoutException;
import com.englishvoicecoach.service.PiperUnavailableException;
import com.englishvoicecoach.service.InvalidTtsRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidMessageException.class)
    ResponseEntity<Map<String, String>> invalidMessage(InvalidMessageException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_MESSAGE", "message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidVoiceRequestException.class)
    ResponseEntity<Map<String, String>> invalidVoiceRequest(InvalidVoiceRequestException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_VOICE_REQUEST", "message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTtsRequestException.class)
    ResponseEntity<Map<String, String>> invalidTtsRequest(InvalidTtsRequestException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_TTS_REQUEST", "message", exception.getMessage()));
    }

    @ExceptionHandler(LocalAiUnavailableException.class)
    ResponseEntity<Map<String, String>> localAiUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "LOCAL_AI_UNAVAILABLE",
                        "message", "The local AI service is unavailable. Make sure Ollama is running."));
    }

    @ExceptionHandler(ModelResponseException.class)
    ResponseEntity<Map<String, String>> malformedModelResponse() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "INVALID_LOCAL_AI_RESPONSE",
                        "message", "The local AI returned an invalid response. Please try again."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> unreadableRequest() {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_REQUEST", "message", "Request body must be valid JSON."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalidAudio(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_AUDIO", "message", exception.getMessage()));
    }

    @ExceptionHandler(PiperUnavailableException.class)
    ResponseEntity<Map<String, String>> piperUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "PIPER_UNAVAILABLE", "message", "Local Piper is unavailable."));
    }

    @ExceptionHandler(PiperTimeoutException.class)
    ResponseEntity<Map<String, String>> piperTimeout() {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of("error", "PIPER_TIMEOUT", "message", "Local Piper took too long to respond."));
    }

    @ExceptionHandler(PiperProcessException.class)
    ResponseEntity<Map<String, String>> piperFailure() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "PIPER_FAILURE", "message", "Local Piper returned invalid audio."));
    }

    @ExceptionHandler(SttUnavailableException.class)
    ResponseEntity<Map<String, String>> sttUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "STT_UNAVAILABLE",
                        "message", "Speech recognition service is unavailable. Please make sure the local STT service is running."));
    }

    @ExceptionHandler(SttResponseException.class)
    ResponseEntity<Map<String, String>> invalidSttResponse() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "INVALID_STT_RESPONSE",
                        "message", "The speech recognition service returned an invalid response."));
    }
}
