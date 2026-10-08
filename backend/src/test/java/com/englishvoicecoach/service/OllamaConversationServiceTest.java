package com.englishvoicecoach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.englishvoicecoach.model.GrammarCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OllamaConversationServiceTest {

    @Test
    void validatesPastTenseCorrection() {
        OllamaConversationService service = serviceReturning("""
                {
                  "correctedMessage":"Yesterday I went to college.",
                  "grammarCorrect":false,
                  "errors":[{"originalText":"go","correctedText":"went","category":"TENSE","explanation":"Yesterday shows a completed past action.","grammarRule":"Use the simple past for completed actions in the past.","example":"I went to the market yesterday."}],
                  "naturalnessSuggestion":null,
                  "conversationResponse":"What did you do at college?",
                  "needsCorrectionPractice":true
                }
                """);

        var response = service.respond("Yesterday I go to college.");

        assertThat(response.grammarCorrect()).isFalse();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().category()).isEqualTo(GrammarCategory.TENSE);
        assertThat(response.needsCorrectionPractice()).isTrue();
    }

    @Test
    void acceptsCorrectSentenceWithoutErrors() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"I went to college yesterday.",
                "grammarCorrect":true,
                "errors":[],
                "naturalnessSuggestion":null,
                "conversationResponse":"What did you study there?",
                "needsCorrectionPractice":false
                """));

        var response = service.respond("I went to college yesterday.");

        assertThat(response.grammarCorrect()).isTrue();
        assertThat(response.errors()).isEmpty();
        assertThat(response.needsCorrectionPractice()).isFalse();
    }

    @Test
    void preservesNaturalnessSuggestionAsGrammarCorrect() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"I am having a doubt.",
                "grammarCorrect":true,
                "errors":[],
                "naturalnessSuggestion":"I have a question.",
                "conversationResponse":"What would you like to ask?",
                "needsCorrectionPractice":false
                """));

        var response = service.respond("I am having a doubt.");

        assertThat(response.grammarCorrect()).isTrue();
        assertThat(response.naturalnessSuggestion()).isEqualTo("I have a question.");
    }

    @Test
    void doesNotClassifyKnownNaturalnessExampleAsGrammarError() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"I have a question.",
                "grammarCorrect":false,
                "errors":[{"originalText":"having a doubt","correctedText":"have a question","category":"VOCABULARY","explanation":"Use different words.","grammarRule":"Use natural vocabulary.","example":"I have a question."}],
                "naturalnessSuggestion":null,
                "conversationResponse":"What would you like to ask?",
                "needsCorrectionPractice":true
                """));

        var response = service.respond("I am having a doubt.");

        assertThat(response.grammarCorrect()).isTrue();
        assertThat(response.errors()).isEmpty();
        assertThat(response.naturalnessSuggestion()).isEqualTo("I have a question.");
        assertThat(response.needsCorrectionPractice()).isFalse();
    }

    @Test
    void validatesSubjectVerbAgreementCorrection() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"He doesn't like coffee.",
                "grammarCorrect":false,
                "errors":[{"originalText":"don't","correctedText":"doesn't","category":"SUBJECT_VERB_AGREEMENT","explanation":"He takes does not.","grammarRule":"Use does not with he, she, and it.","example":"He doesn't like coffee."}],
                "naturalnessSuggestion":null,
                "conversationResponse":"What kind of coffee does he like?",
                "needsCorrectionPractice":true
                """));

        var response = service.respond("He don't like coffee.");

        assertThat(response.correctedMessage()).isEqualTo("He doesn't like coffee.");
        assertThat(response.errors().getFirst().category()).isEqualTo(GrammarCategory.SUBJECT_VERB_AGREEMENT);
    }

    @Test
    void validatesAuxiliaryAndModalCorrections() {
        OllamaConversationService didService = serviceReturning(validResponse("""
                "correctedMessage":"What did you go there for?",
                "grammarCorrect":false,
                "errors":[{"originalText":"went","correctedText":"go","category":"AUXILIARY_VERB","explanation":"Did is followed by the base verb.","grammarRule":"Use the base verb after did.","example":"What did you go there for?"}],
                "naturalnessSuggestion":null,
                "conversationResponse":"Were you looking for something?",
                "needsCorrectionPractice":true
                """));
        OllamaConversationService modalService = serviceReturning(validResponse("""
                "correctedMessage":"He can go there.",
                "grammarCorrect":false,
                "errors":[{"originalText":"goes","correctedText":"go","category":"MODAL_VERB","explanation":"Can is followed by the base verb.","grammarRule":"Use the base verb after a modal verb.","example":"He can go there."}],
                "naturalnessSuggestion":null,
                "conversationResponse":"Why does he need to go?",
                "needsCorrectionPractice":true
                """));

        assertThat(didService.respond("What did you went there for?").correctedMessage())
                .isEqualTo("What did you go there for?");
        assertThat(modalService.respond("He can goes there.").correctedMessage())
                .isEqualTo("He can go there.");
    }

    @Test
    void validatesPastParticipleCorrection() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"I have gone to college.",
                "grammarCorrect":false,
                "errors":[{"originalText":"went","correctedText":"gone","category":"TENSE","explanation":"Have is followed by a past participle.","grammarRule":"Use have or has with the past participle in the present perfect.","example":"I have gone to college."}],
                "naturalnessSuggestion":null,
                "conversationResponse":"What did you study there?",
                "needsCorrectionPractice":true
                """));

        assertThat(service.respond("I have went to college.").correctedMessage())
                .isEqualTo("I have gone to college.");
    }

    @Test
    void rejectsMalformedJson() {
        OllamaConversationService service = serviceReturning("{not-json");

        assertThatThrownBy(() -> service.respond("Hello"))
                .isInstanceOf(ModelResponseException.class)
                .hasMessage("Ollama returned malformed JSON.");
    }

    @Test
    void rejectsInvalidCategory() {
        OllamaConversationService service = serviceReturning(validResponse("""
                "correctedMessage":"He doesn't like coffee.",
                "grammarCorrect":false,
                "errors":[{"originalText":"don't","correctedText":"doesn't","category":"MADE_UP","explanation":"Agreement.","grammarRule":"Use does not with he.","example":"He doesn't like coffee."}],
                "naturalnessSuggestion":null,
                "conversationResponse":"What coffee do you like?",
                "needsCorrectionPractice":true
                """));

        assertThatThrownBy(() -> service.respond("He don't like coffee."))
                .isInstanceOf(ModelResponseException.class);
    }

    @Test
    void rejectsEmptyModelResponse() {
        OllamaConversationService service = new OllamaConversationService(
                prompt -> {
                    throw new ModelResponseException("Ollama returned an empty response.");
                },
                new ObjectMapper());

        assertThatThrownBy(() -> service.respond("Hello"))
                .isInstanceOf(ModelResponseException.class);
    }

    @Test
    void propagatesOllamaUnavailableError() {
        OllamaConversationService service = new OllamaConversationService(
                prompt -> {
                    throw new LocalAiUnavailableException("Unable to connect to Ollama.");
                },
                new ObjectMapper());

        assertThatThrownBy(() -> service.respond("Hello"))
                .isInstanceOf(LocalAiUnavailableException.class);
    }

    private String validResponse(String fields) {
        return "{" + fields + "}";
    }

    private OllamaConversationService serviceReturning(String response) {
        return new OllamaConversationService(prompt -> response, new ObjectMapper());
    }
}
