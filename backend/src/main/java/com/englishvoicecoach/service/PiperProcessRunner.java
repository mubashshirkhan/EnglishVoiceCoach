package com.englishvoicecoach.service;

import java.nio.file.Path;

public interface PiperProcessRunner {

    void synthesize(String text, Path outputFile);
}
