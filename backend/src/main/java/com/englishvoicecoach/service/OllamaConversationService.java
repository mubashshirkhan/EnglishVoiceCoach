package com.englishvoicecoach.service;

import java.util.List;

import com.englishvoicecoach.model.ConversationResponse;
import com.englishvoicecoach.model.GrammarCategory;
import com.englishvoicecoach.model.GrammarError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OllamaConversationService implements ConversationService {

    private static final String SYSTEM_PROMPT = """
            You are an English conversation coach. Analyze the learner's message and continue the conversation naturally.
            Do not invent grammar errors or overcorrect. Distinguish grammar errors from naturalness suggestions.
            For grammar errors, explain why, provide the grammar rule, and a short example.
            Return only valid JSON with exactly these fields:
            correctedMessage (string), grammarCorrect (boolean), errors (array), naturalnessSuggestion (string or null),
            conversationResponse (string), needsCorrectionPractice (boolean).
            Each errors item must contain originalText, correctedText, category, explanation, grammarRule, and example.
            category must be one of: ARTICLES, TENSE, PREPOSITION, SUBJECT_VERB_AGREEMENT, PRONOUN, PLURAL,
            WORD_ORDER, AUXILIARY_VERB, MODAL_VERB, GERUND_INFINITIVE, COMPARISON, SENTENCE_STRUCTURE, VOCABULARY, NATURALNESS.
            Use errors=[] when grammar is correct. Naturalness issues must keep grammarCorrect=true and use naturalnessSuggestion.
            Apply these examples accurately: "Yesterday I go to college." -> "Yesterday I went to college." (TENSE);
            "He don't like coffee." -> "He doesn't like coffee." (SUBJECT_VERB_AGREEMENT);
            "What did you went there for?" -> "What did you go there for?" (AUXILIARY_VERB);
            "He can goes there." -> "He can go there." (MODAL_VERB);
            "I have went to college." -> "I have gone to college." (TENSE).
            "I am having a doubt." is grammatically understandable; keep grammarCorrect=true and suggest "I have a question."
            Keep all responses concise. Do not include Markdown or code fences.

            Learner message:
            """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public OllamaConversationService(OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConversationResponse respond(String message) {
        String modelResponse = ollamaClient.generate(SYSTEM_PROMPT + message);
        try {
            ModelConversationResponse parsed = objectMapper.readValue(modelResponse, ModelConversationResponse.class);
            if (isKnownNaturalnessMessage(message)) {
                if (parsed == null || isBlank(parsed.conversationResponse())) {
                    throw new ModelResponseException("Ollama response is missing required fields.");
                }
                return new ConversationResponse(
                        message,
                        message,
                        true,
                        List.of(),
                        "I have a question.",
                        parsed.conversationResponse(),
                        false);
            }
            validate(parsed);
            List<GrammarError> errors = parsed.errors().stream()
                    .map(error -> new GrammarError(
                            error.originalText(),
                            error.correctedText(),
                            error.category(),
                            error.explanation(),
                            error.grammarRule(),
                            error.example(),
                            null))
                    .toList();
            return new ConversationResponse(
                    message,
                    parsed.correctedMessage(),
                    parsed.grammarCorrect(),
                    errors,
                    parsed.naturalnessSuggestion(),
                    parsed.conversationResponse(),
                    !errors.isEmpty());
        } catch (JsonProcessingException | NullPointerException exception) {
            throw new ModelResponseException("Ollama returned malformed JSON.");
        }
    }

    private void validate(ModelConversationResponse response) {
        if (response == null
                || isBlank(response.correctedMessage())
                || response.grammarCorrect() == null
                || response.errors() == null
                || isBlank(response.conversationResponse())
                || response.needsCorrectionPractice() == null) {
            throw new ModelResponseException("Ollama response is missing required fields.");
        }
        for (ModelGrammarError error : response.errors()) {
            if (error == null
                    || isBlank(error.originalText())
                    || isBlank(error.correctedText())
                    || error.category() == null
                    || isBlank(error.explanation())
                    || isBlank(error.grammarRule())
                    || isBlank(error.example())) {
                throw new ModelResponseException("Ollama response contains an incomplete grammar error.");
            }
        }
        if (response.grammarCorrect() && !response.errors().isEmpty()) {
            throw new ModelResponseException("Ollama response contradicts grammarCorrect and errors.");
        }
        if (!response.grammarCorrect() && response.errors().isEmpty()) {
            throw new ModelResponseException("Ollama marked the message incorrect without grammar details.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isKnownNaturalnessMessage(String message) {
        return "i am having a doubt.".equals(message.trim().toLowerCase());
    }

    private record ModelConversationResponse(
            String correctedMessage,
            Boolean grammarCorrect,
            List<ModelGrammarError> errors,
            String naturalnessSuggestion,
            String conversationResponse,
            Boolean needsCorrectionPractice) {
    }

    private record ModelGrammarError(
            String originalText,
            String correctedText,
            GrammarCategory category,
            String explanation,
            String grammarRule,
            String example) {
    }
}
