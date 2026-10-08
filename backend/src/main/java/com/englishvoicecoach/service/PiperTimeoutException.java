package com.englishvoicecoach.service;

public class PiperTimeoutException extends RuntimeException {

    public PiperTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
