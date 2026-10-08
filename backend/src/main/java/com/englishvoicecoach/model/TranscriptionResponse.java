package com.englishvoicecoach.model;

public record TranscriptionResponse(
        String text,
        String language,
        double duration,
        double transcriptionDuration) {
}
