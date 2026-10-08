package com.englishvoicecoach.service;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

@Service
public class LocalPiperTtsService implements TtsService {

    private final PiperProcessRunner processRunner;

    public LocalPiperTtsService(PiperProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidTtsRequestException("Text must not be blank.");
        }
        try {
            var outputFile = Files.createTempFile("english-voice-coach-", ".wav");
            try {
                processRunner.synthesize(text.trim(), outputFile);
                byte[] audio = Files.readAllBytes(outputFile);
                if (audio.length < 44 || audio[0] != 'R' || audio[1] != 'I'
                        || audio[2] != 'F' || audio[3] != 'F') {
                    throw new PiperProcessException("Piper returned invalid or empty WAV audio.");
                }
                return audio;
            } finally {
                Files.deleteIfExists(outputFile);
            }
        } catch (PiperProcessException | PiperUnavailableException | PiperTimeoutException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PiperProcessException("Unable to read Piper audio output.");
        }
    }
}
