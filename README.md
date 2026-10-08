# English Voice Coach

English Voice Coach is a local-first learning application for practicing English through natural conversation. Phase 6 completes the local voice loop: browser recording is transcribed, analyzed, synthesized by Piper, and played back in the browser.

## Architecture

```text
🎤 Microphone
    ↓
Whisper / faster-whisper
    ↓
Spring Boot
    ↓
Ollama (Qwen2.5:3B)
    ↓
Piper
    ↓
🔊 Speaker
```

Speech-to-text is implemented as a separate local service. Piper runs locally
through the Spring Boot TTS adapter and browser playback uses returned WAV audio.

## Technology stack

- Frontend: React, Vite, JavaScript
- Backend: Java, Spring Boot, Maven Wrapper
- Database: SQLite architecture reserved for a future phase
- Local AI: Ollama configuration documented for a future phase

## Current development phase

**Phase 6 — Complete Local Voice Conversation Loop**

The current phase provides:

- A clean React/Vite frontend
- A minimal Spring Boot backend
- `GET /api/health`
- Frontend-to-backend development connectivity
- `POST /api/conversation/message`
- Structured grammar and naturalness feedback
- Local Ollama integration using `qwen2.5:3b`
- A functional text conversation UI with loading and error states
- Browser MediaRecorder microphone capture
- Spring Boot `POST /api/transcribe` proxy
- Local Python faster-whisper service
- Dedicated `POST /api/voice/conversation` orchestration endpoint
- Temporary in-memory voice sessions with normal and correction-practice states
- Voice transcripts submitted exactly once per completed recording
- Local Piper speech synthesis for generated voice responses
- Browser playback of WAV responses from `POST /api/tts/speak`
- Explicit frontend voice states for recording, processing, speaking,
  correction practice, errors, and idle
- Single-submission recording guards and stale-response protection
- Microphone-track, audio, object-URL, and component-unmount cleanup

The frontend does not automatically start a new recording after playback.
The learner presses the microphone button for each turn. Text chat remains
separate from the voice session.

## Prerequisites

- Java 25
- Node.js 22 or newer
- npm
- Git
- Python 3.13.7

Maven does not need to be installed globally. Use the Maven Wrapper from the `backend` directory.

## Run the backend

From the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080`. Check it with:

```powershell
curl http://localhost:8080/api/health
```

Expected response:

```json
{"status":"ok"}
```

## Run the frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`.

The Vite development proxy forwards `/api` requests to the backend at `http://localhost:8080`.

## Start local speech-to-text

The STT service uses faster-whisper and loads one CPU model at startup:

```powershell
cd stt
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
$env:WHISPER_MODEL="small"
python -m uvicorn app:app --host 127.0.0.1 --port 8000
```

The initial `small` model is configurable with `WHISPER_MODEL`; use
`WHISPER_MODEL=base` if CPU memory or latency requires a smaller model.
Defaults are `WHISPER_DEVICE=cpu` and `WHISPER_COMPUTE_TYPE=int8`.
The first start downloads the model if it is not already cached.

The service provides `GET http://127.0.0.1:8000/health` and
`POST http://127.0.0.1:8000/transcribe`. Browser WebM/Opus recordings are
supported, along with Ogg, WAV, MP3, MP4/M4A, and AAC. Uploads are limited
to 25 MB.

The browser microphone flow is:

```text
MediaRecorder → Spring Boot /api/voice/conversation
    → Python /transcribe → transcript → voice session logic
    → ConversationService → Ollama/Qwen2.5:3B
```

Click **Start Recording**, allow microphone permission, speak, then click
**Stop Recording**. The recording is sent once with a generated session ID.
The voice endpoint owns transcription, correction practice, and the
conversation response.

Voice endpoint request:

```http
POST /api/voice/conversation
Content-Type: multipart/form-data
```

Fields: `audio` and `sessionId`. Voice sessions are temporary and in-memory;
they are isolated by session ID and use `NORMAL_CONVERSATION` and
`CORRECTION_PRACTICE` states.

## Start local Piper TTS

Phase 5 uses `piper-tts==1.8.0` in the dedicated `tts/.venv` environment and
the local `en_US-lessac-medium` voice. The model files are stored under
`tts/voices/` and are excluded from Git.

```powershell
cd tts
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
python -m piper.download_voices --data-dir .\voices en_US-lessac-medium
```

The Spring Boot defaults expect:

```text
PIPER_EXECUTABLE=..\tts\.venv\Scripts\piper.exe
PIPER_MODEL=..\tts\voices\en_US-lessac-medium.onnx
PIPER_TIMEOUT_MS=30000
```

Override these environment variables only when the local Piper executable or
voice model is stored elsewhere. The backend endpoint is:

```http
POST /api/tts/speak
Content-Type: application/json
```

```json
{"text":"Perfect! What did you do at college?"}
```

It returns a local PCM WAV with `Content-Type: audio/wav`. Voice conversation
responses are sent to this endpoint by React after the Phase 4 response arrives;
TTS failure is displayed as a playback error and does not alter the voice
session state.

Run STT unit tests without model inference:

```powershell
cd stt
python -m pytest
```

## Ollama configuration

Set these environment variables when overriding the local defaults:

```text
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
```

Start Ollama locally before using the text conversation engine:

```powershell
ollama serve
ollama list
```

The backend calls Ollama; React never calls Ollama directly. The default request uses `http://localhost:11434/api/generate`, the `qwen2.5:3b` model, JSON output mode, and a low temperature for reliable structured responses.

## Conversation API

Request:

```http
POST /api/conversation/message
Content-Type: application/json
```

```json
{"message":"Yesterday I go to college."}
```

Example response:

```json
{
  "originalMessage": "Yesterday I go to college.",
  "correctedMessage": "Yesterday I went to college.",
  "grammarCorrect": false,
  "errors": [
    {
      "originalText": "go",
      "correctedText": "went",
      "category": "TENSE",
      "explanation": "Yesterday shows a completed past action.",
      "grammarRule": "Use the simple past for completed actions in the past.",
      "example": "I went to the market yesterday.",
      "noteId": null
    }
  ],
  "naturalnessSuggestion": null,
  "conversationResponse": "What did you do at college?",
  "needsCorrectionPractice": true
}
```

Malformed model responses, unavailable Ollama, empty responses, invalid categories, and invalid learner requests return structured API errors without exposing stack traces.

## Future architecture

```text
🎤
 ↓
Whisper / faster-whisper
 ↓
Spring Boot REST API
 ↓
Ollama Qwen2.5:3B
 ↓
Piper
 ↓
🔊
```

The selected `small` model must be cached locally before starting the STT service.
SQLite persistence, conversation history, authentication, cloud deployment,
analytics, pronunciation scoring, and the Grammar Library remain future work.
