package com.englishvoicecoach.service;

import java.util.Map;
import java.util.List;

import com.englishvoicecoach.config.OllamaProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaHttpClient implements OllamaClient {

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", responseProperties(),
            "required", List.of(
                    "correctedMessage", "grammarCorrect", "errors", "naturalnessSuggestion",
                    "conversationResponse", "needsCorrectionPractice"));

    private static Map<String, Object> responseProperties() {
        return Map.of(
                "correctedMessage", Map.of("type", "string"),
                "grammarCorrect", Map.of("type", "boolean"),
                "errors", Map.of("type", "array", "items", errorSchema()),
                "naturalnessSuggestion", Map.of("type", List.of("string", "null")),
                "conversationResponse", Map.of("type", "string"),
                "needsCorrectionPractice", Map.of("type", "boolean"));
    }

    private static Map<String, Object> errorSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "originalText", Map.of("type", "string"),
                        "correctedText", Map.of("type", "string"),
                        "category", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "grammarRule", Map.of("type", "string"),
                        "example", Map.of("type", "string")),
                "required", List.of(
                        "originalText", "correctedText", "category",
                        "explanation", "grammarRule", "example"));
    }

    private final RestClient restClient;
    private final OllamaProperties properties;
    public OllamaHttpClient(@Qualifier("ollamaRestClient") RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    @Override
    public String generate(String prompt) {
        try {
            OllamaResponse response = restClient.post()
                    .uri("/api/generate")
                    .body(Map.of(
                            "model", properties.model(),
                            "prompt", prompt,
                            "stream", false,
                            "format", RESPONSE_SCHEMA,
                            "options", Map.of("temperature", 0.2)))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new LocalAiUnavailableException("Ollama returned HTTP " + clientResponse.getStatusCode());
                    })
                    .body(OllamaResponse.class);

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw new ModelResponseException("Ollama returned an empty response.");
            }
            return response.response();
        } catch (LocalAiUnavailableException | ModelResponseException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new LocalAiUnavailableException("Unable to connect to Ollama.", exception);
        }
    }

    private record OllamaResponse(String response) {
    }
}
