package com.englishvoicecoach.model;

public record VoiceConversationResponse(
        String sessionId,
        String transcript,
        VoiceSessionState state,
        String correctedSentence,
        ConversationResponse grammar,
        String conversationResponse,
        boolean needsCorrectionPractice) {
}
