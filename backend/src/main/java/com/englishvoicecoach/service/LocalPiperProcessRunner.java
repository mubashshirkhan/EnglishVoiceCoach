package com.englishvoicecoach.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.englishvoicecoach.config.TtsProperties;
import org.springframework.stereotype.Component;

@Component
public class LocalPiperProcessRunner implements PiperProcessRunner {

    private final TtsProperties properties;

    public LocalPiperProcessRunner(TtsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void synthesize(String text, Path outputFile) {
        Process process;
        try {
            process = new ProcessBuilder(List.of(
                    properties.executable(),
                    "--model", properties.model(),
                    "--output_file", outputFile.toString()))
                    .redirectErrorStream(true)
                    .start();
            try (var writer = process.outputWriter(StandardCharsets.UTF_8)) {
                writer.write(text);
                writer.write(System.lineSeparator());
            }
            if (!process.waitFor(properties.timeoutMs(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new PiperTimeoutException("Piper synthesis timed out.", null);
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new PiperProcessException("Piper synthesis failed: " + output);
            }
        } catch (PiperTimeoutException | PiperProcessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PiperUnavailableException("Piper executable or model is unavailable.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PiperTimeoutException("Piper synthesis was interrupted.", exception);
        }
    }
}
