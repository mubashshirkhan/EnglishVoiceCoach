package com.englishvoicecoach.service;

public class SttUnavailableException extends RuntimeException {

    public SttUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
