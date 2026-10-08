package com.englishvoicecoach.model;

public final class VoiceSession {

    private final String sessionId;
    private VoiceSessionState state = VoiceSessionState.NORMAL_CONVERSATION;
    private String expectedCorrectedSentence;

    public VoiceSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String sessionId() {
        return sessionId;
    }

    public synchronized VoiceSessionState state() {
        return state;
    }

    public synchronized String expectedCorrectedSentence() {
        return expectedCorrectedSentence;
    }

    public synchronized void beginCorrectionPractice(String correctedSentence) {
        state = VoiceSessionState.CORRECTION_PRACTICE;
        expectedCorrectedSentence = correctedSentence;
    }

    public synchronized void resumeNormalConversation() {
        state = VoiceSessionState.NORMAL_CONVERSATION;
        expectedCorrectedSentence = null;
    }
}
