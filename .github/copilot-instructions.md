# English Voice Coach — Copilot Instructions

## Read first

Before changing code, read:

- `README.md`
- `DEVELOPMENT_LOG.md`
- `CLAUDE.md`
- this file

Do not redo completed phases or rebuild the project from scratch. Preserve
working behavior and make the smallest safe incremental change.

## Project goal

English Voice Coach is a free, local-first English conversation and grammar
coach. A learner speaks into the browser, receives grammar-aware conversation
feedback, and hears the response through local text-to-speech.

The intended architecture is:

```text
Browser microphone
  -> React MediaRecorder
  -> Spring Boot /api/voice/conversation
  -> local faster-whisper STT
  -> voice session and correction logic
  -> Ollama Qwen2.5:3B
  -> Spring Boot /api/tts/speak
  -> local Piper TTS
  -> browser speaker
```

## Non-negotiable constraints

- Keep the project local-first and free by default.
- Do not use or add OpenAI API, Gemini API, Claude API, or paid cloud AI.
- Do not replace local AI components with cloud services.
- Do not download or resolve the Whisper model through Hugging Face.
- Do not redesign the working architecture without a concrete, verified reason.
- Make incremental, surgical changes.
- Do not claim a behavior works without actually verifying it.
- Run the relevant tests, builds, or live checks after changes.
- Do not modify unrelated pre-existing issues.

## Technology and environment

- Frontend: React and Vite.
- Backend: Java Spring Boot with Maven Wrapper.
- STT: local Python faster-whisper.
- LLM: local Ollama `qwen2.5:3b`.
- TTS: local Piper.
- Target environment: Windows 11 x64.
- Verified Java: 25 LTS.
- Verified Python: 3.13.7.
- Verified faster-whisper: 1.2.1.
- Verified CTranslate2: 4.8.2.
- Verified PyAV: 16.1.0.
- Node and npm are installed; use the project scripts.
- Maven is not installed globally; use `backend\mvnw.cmd`.

## Completed phases

### Phase 1 — Foundation

Completed the Spring Boot backend, React/Vite frontend, project structure,
documentation, and `GET /api/health`.

### Phase 2 — Text conversation

Completed the text pipeline:

```text
React -> Spring Boot -> Ollama Qwen2.5:3B
```

Grammar handling distinguishes:

- grammatically incorrect;
- grammatically correct but unnatural; and
- correct and natural.

Grammar errors contain the original sentence, corrected sentence, explanation
of why it is wrong, the grammar rule, and an example. Keep text mode separate
from voice mode.

### Phase 3 — Local faster-whisper STT

Completed the local STT service and Spring Boot integration. The model is
`faster-whisper-small` and is stored in the existing ModelScope cache.

The STT service must load the existing local model directory directly with
`local_files_only=True`. Preserve CPU/int8 configuration unless a verified
runtime requirement proves a change is necessary. Never fall back to a
Hugging Face download.

### Phase 4 — Voice conversation

Completed `POST /api/voice/conversation`, accepting `audio` and `sessionId`.
Voice sessions use:

- `NORMAL_CONVERSATION`
- `CORRECTION_PRACTICE`

Correction practice stores the expected corrected sentence and requires the
learner to repeat it correctly before returning to normal conversation.

### Phase 5 — Local Piper TTS

Completed local Piper TTS. Spring Boot exposes `/api/tts/speak`. Keep Piper
local and preserve its process, timeout, output-validation, and cleanup
behavior.

### Phase 6 — Browser voice loop

The complete browser voice pipeline is being integrated. The React recording
implementation already includes explicit recording/processing/speaking states,
stable session IDs, one-submission protection, microphone cleanup, audio URL
cleanup, stale TTS protection, and text preservation when TTS fails.

## Important browser diagnosis

The original microphone recording bug was caused by React development
StrictMode replaying the mount-effect cleanup:

1. cleanup set `mountedRef.current = false`;
2. the replayed effect setup did not restore it;
3. after `getUserMedia()` resolved, the valid stream was treated as stale;
4. MediaRecorder construction was skipped or the stream was stopped too early.

The fix is to set `mountedRef.current = true` during effect setup. Do not
remove or casually refactor this lifecycle fix.

Browser verification has established:

- `getUserMedia()` works;
- `MediaRecorder` works;
- `audio/webm;codecs=opus` works;
- recording starts and Stop Recording works;
- a non-zero Blob is produced; and
- exactly one `POST /api/voice/conversation` is generated per recording.

## Latest STT verification

`stt/app.py` now loads the existing local ModelScope
`faster-whisper-small` snapshot directly with `local_files_only=True`.

Verified:

- the local model directory exists;
- `model.bin` exists;
- `GET /health` returns HTTP 200;
- real local audio transcribed successfully as
  `Yesterday I went to college.`;
- browser-generated WebM/Opus reached STT and was decoded; and
- a silent/no-speech request returned the expected 422, not a format error.

Do not delete the model cache or change the model unless it is genuinely
unavailable and the change is explicitly justified.

## Latest test baseline

- STT: 7 tests passed.
- Spring Boot backend: 38 tests passed.
- Frontend production build: passed.

The STT suite includes content-type and WebM codec-parameter coverage, but
unit tests do not replace live production verification. A live end-to-end
claim must reach STT, Spring Boot, Qwen, Piper, and browser playback.

## Next goal: real browser E2E verification

Complete and document a real browser voice run:

```text
Microphone
  -> MediaRecorder
  -> /api/voice/conversation
  -> faster-whisper
  -> Qwen2.5:3B
  -> Piper TTS
  -> browser speaker
```

At minimum verify:

1. `Yesterday I go to college.` enters correction practice.
2. `Yesterday I went to college.` is accepted and returns to normal
   conversation.
3. An incorrect repetition keeps correction practice active.

For each completed recording, verify one request only, a stable `sessionId`,
released microphone tracks, cleaned-up object URLs, and no stale TTS playback.
STT failure must not call Qwen or Piper. Qwen failure must not call Piper.
An empty transcript must not call Qwen or Piper. If TTS fails, preserve and
display the text response.

## Change and verification guidance

- Prefer existing helpers, endpoint contracts, state names, and patterns.
- Keep `/api/voice/conversation` separate from `/api/tts/speak`.
- Keep text mode separate from voice mode.
- Do not introduce cloud services, authentication, accounts, or unrelated
  persistence.
- For STT changes, verify the actual local model path and run STT tests.
- For backend changes, run `backend\mvnw.cmd test`.
- For frontend changes, run `npm run build` from `frontend`.
- For browser behavior, inspect the browser console and Network tab; do not
  infer request counts from a successful build.
- Report failures precisely rather than hiding them with fallback success.
- Do not commit unless the user explicitly requests a commit.
