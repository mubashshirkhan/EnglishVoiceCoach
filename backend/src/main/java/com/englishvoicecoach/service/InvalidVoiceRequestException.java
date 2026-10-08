package com.englishvoicecoach.service;

public class InvalidVoiceRequestException extends RuntimeException {

    public InvalidVoiceRequestException(String message) {
        super(message);
    }
}
