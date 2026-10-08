package com.englishvoicecoach.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.englishvoicecoach.model.VoiceSession;
import org.springframework.stereotype.Component;

@Component
public class VoiceSessionStore {

    private final ConcurrentMap<String, VoiceSession> sessions = new ConcurrentHashMap<>();

    public VoiceSession getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, VoiceSession::new);
    }
}
