package com.englishvoicecoach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.englishvoicecoach.model.ConversationResponse;
import com.englishvoicecoach.model.GrammarCategory;
import com.englishvoicecoach.model.GrammarError;
import com.englishvoicecoach.model.TranscriptionResponse;
import com.englishvoicecoach.model.VoiceSessionState;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class VoiceConversationServiceTest {

    @Test
    void normalVoiceConversationStaysNormal() {
        AtomicInteger qwenCalls = new AtomicInteger();
        VoiceConversationService service = serviceFor(
                "Yesterday I went to college.",
                correctResponse("That's good! What did you do at college?"),
                qwenCalls);

        var response = service.converse(audio(), "normal");

        assertThat(response.state()).isEqualTo(VoiceSessionState.NORMAL_CONVERSATION);
        assertThat(response.needsCorrectionPractice()).isFalse();
        assertThat(response.conversationResponse()).contains("What did you do");
        assertThat(qwenCalls).hasValue(1);
    }

    @Test
    void grammarErrorStoresCorrectionAndActivatesPractice() {
        VoiceConversationService service = serviceFor(
                "Yesterday I go to college.",
                errorResponse("Yesterday I went to college.", GrammarCategory.TENSE),
                new AtomicInteger());

        var response = service.converse(audio(), "practice");

        assertThat(response.state()).isEqualTo(VoiceSessionState.CORRECTION_PRACTICE);
        assertThat(response.correctedSentence()).isEqualTo("Yesterday I went to college.");
        assertThat(response.conversationResponse())
                .isEqualTo("Yesterday I went to college. Can you say that again correctly?");
    }

    @Test
    void correctRepetitionClearsPracticeAndCallsConversationService() {
        AtomicInteger qwenCalls = new AtomicInteger();
        VoiceConversationService service = serviceForSequence(
                List.of("Yesterday I go to college.", "Yesterday I went to college."),
                List.of(
                        errorResponse("Yesterday I went to college.", GrammarCategory.TENSE),
                        correctResponse("What did you do at college?")),
                qwenCalls);

        service.converse(audio(), "repeat");
        var response = service.converse(audio(), "repeat");

        assertThat(response.state()).isEqualTo(VoiceSessionState.NORMAL_CONVERSATION);
        assertThat(response.correctedSentence()).isNull();
        assertThat(response.conversationResponse()).isEqualTo("Perfect! What did you do at college?");
        assertThat(qwenCalls).hasValue(2);
    }

    @Test
    void incorrectRepetitionRemainsInPracticeWithoutCallingQwen() {
        AtomicInteger qwenCalls = new AtomicInteger();
        VoiceConversationService service = serviceForSequence(
                List.of("Yesterday I go to college.", "Yesterday I go college."),
                List.of(errorResponse("Yesterday I went to college.", GrammarCategory.TENSE)),
                qwenCalls);

        service.converse(audio(), "incorrect");
        var response = service.converse(audio(), "incorrect");

        assertThat(response.state()).isEqualTo(VoiceSessionState.CORRECTION_PRACTICE);
        assertThat(response.needsCorrectionPractice()).isTrue();
        assertThat(response.conversationResponse())
                .isEqualTo("Almost. Say: Yesterday I went to college. Try again.");
        assertThat(qwenCalls).hasValue(1);
    }

    @Test
    void punctuationCaseAndWhitespaceAreHarmlessButWordsRemainMeaningful() {
        VoiceConversationService service = serviceForSequence(
                List.of("He don't like coffee.", "  HE DOESN'T LIKE COFFEE!  "),
                List.of(
                        errorResponse("He doesn't like coffee.", GrammarCategory.SUBJECT_VERB_AGREEMENT),
                        correctResponse("What does he usually drink?")),
                new AtomicInteger());

        service.converse(audio(), "agreement");
        var response = service.converse(audio(), "agreement");

        assertThat(response.state()).isEqualTo(VoiceSessionState.NORMAL_CONVERSATION);
        assertThat(VoiceConversationService.normalized("Yesterday I go to college."))
                .isNotEqualTo(VoiceConversationService.normalized("Yesterday I went to college."));
    }

    @Test
    void sessionsAreIsolated() {
        VoiceConversationService service = serviceForSequence(
                List.of("Yesterday I go to college.", "Yesterday I went to college."),
                List.of(errorResponse("Yesterday I went to college.", GrammarCategory.TENSE)),
                new AtomicInteger());

        service.converse(audio(), "first");
        var otherSession = service.converse(audio(), "second");

        assertThat(otherSession.state()).isEqualTo(VoiceSessionState.CORRECTION_PRACTICE);
    }

    @Test
    void emptyTranscriptDoesNotCallQwen() {
        AtomicInteger qwenCalls = new AtomicInteger();
        VoiceConversationService service = serviceFor("", correctResponse("unused"), qwenCalls);

        assertThatThrownBy(() -> service.converse(audio(), "empty"))
                .isInstanceOf(InvalidVoiceRequestException.class);
        assertThat(qwenCalls).hasValue(0);
    }

    @Test
    void missingSessionIdIsRejected() {
        VoiceConversationService service = serviceFor("Hello", correctResponse("Hi"), new AtomicInteger());

        assertThatThrownBy(() -> service.converse(audio(), " "))
                .isInstanceOf(InvalidVoiceRequestException.class);
    }

    private VoiceConversationService serviceFor(
            String transcript, ConversationResponse response, AtomicInteger qwenCalls) {
        return serviceForSequence(List.of(transcript), List.of(response), qwenCalls);
    }

    private VoiceConversationService serviceForSequence(
            List<String> transcripts, List<ConversationResponse> responses, AtomicInteger qwenCalls) {
        AtomicInteger index = new AtomicInteger();
        SpeechToTextService stt = audio -> new TranscriptionResponse(
                transcripts.get(Math.min(index.getAndIncrement(), transcripts.size() - 1)), "en", 1, 1);
        ConversationService conversation = message -> {
            qwenCalls.incrementAndGet();
            return responses.get(Math.min(qwenCalls.get() - 1, responses.size() - 1));
        };
        return new VoiceConversationService(stt, conversation, new VoiceSessionStore());
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1});
    }

    private ConversationResponse errorResponse(String corrected, GrammarCategory category) {
        return new ConversationResponse(
                corrected,
                corrected,
                false,
                List.of(new GrammarError("mistake", "correction", category, "why", "rule", "example", null)),
                null,
                "long explanation should not be spoken",
                true);
    }

    private ConversationResponse correctResponse(String response) {
        return new ConversationResponse("message", "message", true, List.of(), null, response, false);
    }
}
