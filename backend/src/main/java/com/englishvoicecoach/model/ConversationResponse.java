package com.englishvoicecoach.model;

import java.util.List;

public record ConversationResponse(
        String originalMessage,
        String correctedMessage,
        boolean grammarCorrect,
        List<GrammarError> errors,
        String naturalnessSuggestion,
        String conversationResponse,
        boolean needsCorrectionPractice) {
}
