package com.englishvoicecoach.service;

import org.springframework.web.multipart.MultipartFile;

import com.englishvoicecoach.model.TranscriptionResponse;

public interface SpeechToTextService {

    TranscriptionResponse transcribe(MultipartFile audio);
}
