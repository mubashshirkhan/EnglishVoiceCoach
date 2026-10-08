package com.englishvoicecoach.service;

import com.englishvoicecoach.model.VoiceConversationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceConversationOperations {

    VoiceConversationResponse converse(MultipartFile audio, String sessionId);
}
