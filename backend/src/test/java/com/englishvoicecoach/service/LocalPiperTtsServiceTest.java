package com.englishvoicecoach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class LocalPiperTtsServiceTest {

    @Test
    void validTextProducesWavBytes() throws Exception {
        LocalPiperTtsService service = new LocalPiperTtsService((text, output) -> write(output, validWav()));

        byte[] audio = service.synthesize("Hello");

        assertThat(audio).startsWith(new byte[] {'R', 'I', 'F', 'F'});
        assertThat(audio).hasSizeGreaterThan(44);
    }

    @Test
    void rejectsEmptyText() {
        LocalPiperTtsService service = new LocalPiperTtsService((text, output) -> {
        });

        assertThatThrownBy(() -> service.synthesize(" "))
                .isInstanceOf(InvalidTtsRequestException.class);
    }

    @Test
    void rejectsInvalidOutput() throws Exception {
        LocalPiperTtsService service = new LocalPiperTtsService((text, output) -> write(output, new byte[0]));

        assertThatThrownBy(() -> service.synthesize("Hello"))
                .isInstanceOf(PiperProcessException.class);
    }

    @Test
    void propagatesUnavailablePiper() {
        LocalPiperTtsService service = new LocalPiperTtsService((text, output) -> {
            throw new PiperUnavailableException("missing", null);
        });

        assertThatThrownBy(() -> service.synthesize("Hello"))
                .isInstanceOf(PiperUnavailableException.class);
    }

    private byte[] validWav() {
        byte[] wav = new byte[48];
        wav[0] = 'R';
        wav[1] = 'I';
        wav[2] = 'F';
        wav[3] = 'F';
        return wav;
    }

    private static void write(java.nio.file.Path output, byte[] bytes) {
        try {
            Files.write(output, bytes);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
