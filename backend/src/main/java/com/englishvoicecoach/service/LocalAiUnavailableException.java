package com.englishvoicecoach.service;

public class LocalAiUnavailableException extends RuntimeException {

    public LocalAiUnavailableException(String message) {
        super(message);
    }

    public LocalAiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
