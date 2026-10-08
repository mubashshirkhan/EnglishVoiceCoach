package com.englishvoicecoach.model;

public record GrammarError(
        String originalText,
        String correctedText,
        GrammarCategory category,
        String explanation,
        String grammarRule,
        String example,
        String noteId) {
}
