package com.englishvoicecoach.service;

import com.englishvoicecoach.model.ConversationResponse;

public interface ConversationService {

    ConversationResponse respond(String message);
}
