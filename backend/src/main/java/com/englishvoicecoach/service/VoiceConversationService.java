package com.englishvoicecoach.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import com.englishvoicecoach.model.ConversationResponse;
import com.englishvoicecoach.model.TranscriptionResponse;
import com.englishvoicecoach.model.VoiceConversationResponse;
import com.englishvoicecoach.model.VoiceSession;
import com.englishvoicecoach.model.VoiceSessionState;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceConversationService implements VoiceConversationOperations {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private final SpeechToTextService speechToTextService;
    private final ConversationService conversationService;
    private final VoiceSessionStore sessionStore;

    public VoiceConversationService(
            SpeechToTextService speechToTextService,
            ConversationService conversationService,
            VoiceSessionStore sessionStore) {
        this.speechToTextService = speechToTextService;
        this.conversationService = conversationService;
        this.sessionStore = sessionStore;
    }

    @Override
    public VoiceConversationResponse converse(MultipartFile audio, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidVoiceRequestException("sessionId must not be blank.");
        }

        TranscriptionResponse transcription = speechToTextService.transcribe(audio);
        String transcript = transcription.text().trim();
        if (transcript.isEmpty()) {
            throw new InvalidVoiceRequestException("Transcript must not be empty.");
        }

        VoiceSession session = sessionStore.getOrCreate(sessionId.trim());
        if (session.state() == VoiceSessionState.CORRECTION_PRACTICE) {
            return handleCorrectionAttempt(session, transcript);
        }

        ConversationResponse grammar = conversationService.respond(transcript);
        if (!grammar.errors().isEmpty()) {
            session.beginCorrectionPractice(grammar.correctedMessage());
            return response(session, transcript, grammar.correctedMessage(), grammar,
                    conciseCorrection(grammar.correctedMessage()), true);
        }

        return response(session, transcript, null, grammar, grammar.conversationResponse(), false);
    }

    private VoiceConversationResponse handleCorrectionAttempt(VoiceSession session, String transcript) {
        String expected = session.expectedCorrectedSentence();
        if (normalized(transcript).equals(normalized(expected))) {
            session.resumeNormalConversation();
            ConversationResponse grammar = conversationService.respond(transcript);
            return response(session, transcript, null, grammar,
                    conciseSuccess(grammar.conversationResponse()), false);
        }

        return response(session, transcript, expected, null,
                "Almost. Say: " + expected + " Try again.", true);
    }

    private VoiceConversationResponse response(
            VoiceSession session,
            String transcript,
            String correctedSentence,
            ConversationResponse grammar,
            String conversationResponse,
            boolean needsCorrectionPractice) {
        return new VoiceConversationResponse(
                session.sessionId(),
                transcript,
                session.state(),
                correctedSentence,
                grammar,
                conversationResponse,
                needsCorrectionPractice);
    }

    private String conciseCorrection(String correctedSentence) {
        return correctedSentence + " Can you say that again correctly?";
    }

    private String conciseSuccess(String response) {
        if (response == null || response.isBlank()) {
            return "Perfect! What would you like to talk about?";
        }
        return response.startsWith("Perfect") ? response : "Perfect! " + response;
    }

    /*
     * Normalize only presentation differences. Words remain intact, so "go"
     * and "went" cannot compare equal.
     */
    static String normalized(String value) {
        String canonical = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'');
        return NON_ALPHANUMERIC.matcher(canonical).replaceAll(" ").trim().replaceAll("\\s+", " ");
    }
}
