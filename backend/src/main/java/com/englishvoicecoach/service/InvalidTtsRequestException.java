package com.englishvoicecoach.service;

public class InvalidTtsRequestException extends RuntimeException {

    public InvalidTtsRequestException(String message) {
        super(message);
    }
}
