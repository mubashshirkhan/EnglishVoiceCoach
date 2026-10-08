# Development Log

## Phase 1 — Project Foundation

- Created a clean project structure.
- Confirmed Git repository initialization.
- Added a React/Vite frontend.
- Added a Spring Boot backend.
- Added the Maven Wrapper.
- Added the `GET /api/health` endpoint.
- Connected the frontend to the backend health endpoint.
- Added the initial voice-ready conversation UI.
- Documented the Ollama configuration.
- Did not implement voice, speech-to-text, text-to-speech, or microphone recording.

## Phase 2 — Local Text Conversation Engine

- Added a dedicated Ollama HTTP client and conversation service abstraction.
- Added `POST /api/conversation/message`.
- Added structured grammar correction and naturalness feedback.
- Added controlled grammar categories and response validation.
- Added user-friendly errors for unavailable Ollama and malformed model output.
- Added mocked backend tests for valid, invalid, empty, and unavailable model responses.
- Replaced the placeholder frontend with a functional text conversation UI.
- Kept the voice button visible and disabled; no voice functionality was implemented.
- Configured `OLLAMA_BASE_URL=http://localhost:11434` and `OLLAMA_MODEL=qwen2.5:3b`.

## Phase 3 — Local Speech-to-Text Foundation

- Added a dedicated Python faster-whisper service under `stt/`.
- Added configurable `WHISPER_MODEL`, CPU device, and compute type settings.
- Added one-time model loading during STT service startup.
- Added STT `/health` and `/transcribe` endpoints with audio validation.
- Added Spring Boot `POST /api/transcribe` proxy/orchestration.
- Added configurable STT URL and connect/response timeouts.
- Added structured handling for invalid audio, unavailable STT, timeout, and invalid STT responses.
- Replaced the disabled voice placeholder with browser MediaRecorder controls.
- Displayed transcripts without sending them to Qwen/Ollama.
- Added Python fake-model tests and Spring proxy tests.
- Verified Python 3.13.7 and installed `faster-whisper` in the dedicated environment.
- Cached and loaded the real `small` model locally through the verified ModelScope mirror after the Hugging Face connection reset.
- Verified real local transcription and kept the model cache outside version control.
- Did not implement Piper, audio playback, automatic Qwen submission, persistence, authentication, analytics, or pronunciation scoring.

## Phase 4 — Voice-to-Conversation Integration

- Connected successful voice transcripts to the existing `/api/conversation/message` endpoint.
- Reused the existing grammar feedback and conversation response UI for voice and text.
- Fixed the microphone handlers so they are available to the rendered controls.
- Preserved the local-only path: browser → Spring Boot → faster-whisper → transcript → Spring Boot → Ollama/Qwen2.5:3B.
- Kept Piper, text-to-speech, audio playback, persistence, authentication, analytics, and pronunciation scoring out of scope.

## Phase 4A — Stateful Voice Conversation

- Added `POST /api/voice/conversation` for single-request voice orchestration.
- Added temporary concurrent in-memory sessions keyed by `sessionId`.
- Added `NORMAL_CONVERSATION` and `CORRECTION_PRACTICE` states.
- Added expected-correction storage and normalized word comparison for repetitions.
- Added concise voice correction, successful-repetition, and retry responses.
- Added dedicated backend tests for session isolation, state transitions, empty transcripts,
  incorrect repetitions, endpoint fields, and the additional agreement case.
- Updated React voice capture to send one request with `audio` and `sessionId`.

## Phase 5 — Local Piper Text-to-Speech

- Added an isolated local Piper TTS environment using `piper-tts==1.8.0`.
- Added the local `en_US-lessac-medium` voice model outside Git tracking.
- Added Spring Boot `POST /api/tts/speak` returning `audio/wav`.
- Added safe Piper process invocation with argument lists, timeout handling,
  output validation, and temporary-file cleanup.
- Added React playback with object URL release after audio playback.
- Kept Phase 4 STT, Qwen, and session-state behavior unchanged.

## Phase 6 — Complete Local Voice Conversation Loop

- Replaced independent recording/transcription/speaking flags with an explicit
  frontend voice state machine.
- Added reliable MediaRecorder lifecycle handling with one submission per
  completed recording, stale recording guards, empty-recording handling, and
  microphone-track cleanup.
- Added stale TTS request protection, abort handling, audio cleanup, and
  object-URL revocation.
- Preserved successful conversation text when TTS fails and returned the UI to
  a usable error state.
- Displayed the backend correction-practice state and kept the stable voice
  session ID across turns.
- Kept `/api/voice/conversation`, `/api/tts/speak`, and text mode as separate
  endpoints.
- The frontend has no test runner configured; validation uses the production
  build and browser-level request/playback checks rather than adding a large
  testing framework.

## Phase 6 Recording Lifecycle Fix

- Browser tracing found that the direct MediaRecorder API worked, but the
  React recording path stopped the stream before constructing MediaRecorder.
- The cause was the development `StrictMode` effect replay: the unmount
  cleanup set `mountedRef.current` to `false`, and the replayed mount did not
  reset it. The post-`getUserMedia` guard therefore treated every stream as
  stale.
- Resetting the mounted ref during effect setup restored the complete
  `getUserMedia` -> MediaRecorder -> Blob -> voice endpoint flow.
- Added development-stage logging for recorder events, Blob creation, voice
  requests, and TTS outcomes.
- Browser verification then confirmed one non-empty WebM Blob and exactly one
  `POST /api/voice/conversation` request per completed recording.
- The request currently reaches Spring Boot but returns HTTP 503 because the
  local faster-whisper service cannot complete startup/model loading.
- The STT startup failure is:
  `[WinError 10054] An existing connection was forcibly closed by the remote
  host` while the model loader contacts Hugging Face.
- This is the current Phase 6 integration blocker; microphone permissions,
  `getUserMedia`, MediaRecorder, Blob creation, and request submission are
  working.
