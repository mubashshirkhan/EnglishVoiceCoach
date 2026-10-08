package com.englishvoicecoach.service;

import java.util.Set;

import com.englishvoicecoach.config.SttProperties;
import com.englishvoicecoach.model.TranscriptionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalSpeechToTextService implements SpeechToTextService {

    private static final long MAX_AUDIO_BYTES = 25 * 1024 * 1024;
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "audio/webm", "audio/ogg", "audio/wav", "audio/x-wav",
            "audio/mpeg", "audio/mp4", "audio/aac");

    private final RestClient restClient;

    public LocalSpeechToTextService(@Qualifier("sttRestClient") RestClient sttRestClient) {
        this.restClient = sttRestClient;
    }

    @Override
    public TranscriptionResponse transcribe(MultipartFile audio) {
        validate(audio);
        try {
            byte[] bytes = audio.getBytes();
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("audio", new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return audio.getOriginalFilename();
                }
            }).contentType(MediaType.parseMediaType(audio.getContentType()));

            TranscriptionResponse response = restClient.post()
                    .uri("/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(TranscriptionResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()
                    || response.language() == null || response.duration() < 0
                    || response.transcriptionDuration() < 0) {
                throw new SttResponseException("Speech recognition returned an invalid response.");
            }
            return response;
        } catch (SttResponseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SttUnavailableException("Unable to reach the local speech recognition service.", exception);
        }
    }

    private void validate(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required and must not be empty.");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Audio file is too large.");
        }
        String contentType = audio.getContentType();
        if (contentType == null
                || !SUPPORTED_TYPES.contains(contentType.split(";", 2)[0].toLowerCase())) {
            throw new IllegalArgumentException("Unsupported audio format.");
        }
    }
}
