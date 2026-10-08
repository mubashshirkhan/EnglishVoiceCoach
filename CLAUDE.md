# English Voice Coach — Claude Code Project Context

This file is the project-level handoff for future Claude Code sessions. Read
this file, [README.md](./README.md), and
[DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md) before making changes. Preserve
the existing architecture and verify changes with the relevant tests.

## Project goal

English Voice Coach is a local-first, free-by-default English conversation
coach. A learner speaks into the browser, receives local grammar-aware
conversation feedback, and hears the response spoken locally.

Target loop:

```text
Browser microphone
  -> React MediaRecorder
  -> Spring Boot /api/voice/conversation
  -> local faster-whisper STT
  -> voice session/correction logic
  -> Ollama Qwen2.5:3B
  -> concise conversation response
  -> Spring Boot /api/tts/speak
  -> local Piper WAV
  -> browser Audio playback
```

The project must remain local-only. Do not add cloud AI, cloud STT, cloud TTS,
OpenAI, Gemini, Anthropic, Groq, hosted analytics, authentication, or user
accounts.

## Technology and environment

Verified development environment:

- Windows 11 x64
- Java 25 LTS
- Spring Boot 3.4.x
- Maven is not installed globally; use `backend\mvnw.cmd`
- Node.js 22.16.0
- npm 11.4.1
- Python 3.13.7
- Git 2.47.0.windows.2
- Ollama 0.40.0
- Ollama model `qwen2.5:3b`
- Ollama endpoint `http://localhost:11434`
- faster-whisper 1.2.1
- ctranslate2 4.8.2
- PyAV 16.1.0
- Piper package `piper-tts==1.8.0`
- Piper voice `en_US-lessac-medium`

Important local paths:

- Backend: `backend\`
- Frontend: `frontend\`
- STT service: `stt\`
- Piper environment: `tts\.venv\`
- Piper models: `tts\voices\`
- faster-whisper ModelScope cache:
  `stt\modelscope-cache\models\Systran--faster-whisper-small\snapshots\master`

Do not commit virtual environments, model files, model caches, generated WAV
files, or secrets. These paths are ignored by `.gitignore`.

## Phase history

### Phase 1 — Foundation

- Created React/Vite frontend and Spring Boot backend.
- Added Maven Wrapper and clean project structure.
- Added `GET /api/health` returning `{"status":"ok"}`.
- Connected frontend health status to the backend.
- Added initial conversation UI and documented Ollama configuration.
- Intentionally did not implement voice, STT, TTS, persistence, or advanced AI.

### Phase 2 — Local text conversation

- Added Ollama HTTP integration through Spring services.
- Added `POST /api/conversation/message`.
- Added structured grammar correction, naturalness feedback, controlled
  categories, response validation, and structured error handling.
- Preserved text mode as a separate path from voice mode.
- Configured:
  - `OLLAMA_BASE_URL=http://localhost:11434`
  - `OLLAMA_MODEL=qwen2.5:3b`

### Phase 3 — Local speech-to-text

- Added Python FastAPI faster-whisper service in `stt\`.
- Added `/health` and `/transcribe`.
- Added model/device/compute configuration, audio validation, and startup
  model loading.
- Added Spring Boot `/api/transcribe` proxy.
- Added browser MediaRecorder capture and transcript display.
- Installed faster-whisper in the dedicated Python environment.
- A real `small` model was previously downloaded through a ModelScope mirror
  and real local transcription was verified.

### Phase 4 and 4A — Stateful voice conversation

- Added `POST /api/voice/conversation` as the single voice orchestration
  endpoint.
- Request is `multipart/form-data` with `audio` and `sessionId`.
- Added temporary in-memory sessions keyed by stable `sessionId`.
- States:
  - `NORMAL_CONVERSATION`
  - `CORRECTION_PRACTICE`
- Stored expected corrected sentence and enough session context for correction
  practice.
- Used normalized comparison that ignores case, punctuation, apostrophe
  presentation, and whitespace while preserving word differences.
- Example: `go` does not equal `went`.
- Grammar errors produce concise voice responses such as:
  `Yesterday I went to college. Can you say that again correctly?`
- Correct repetition clears correction practice and returns to normal
  conversation.
- Incorrect repetition remains in correction practice without an unnecessary
  second Qwen call.
- Text mode remains on `/api/conversation/message`.

### Phase 5 — Local Piper TTS

- Added dedicated local Piper configuration and process boundary.
- Added `POST /api/tts/speak` with JSON `{"text":"..."}`.
- Returns `audio/wav`.
- Uses safe `ProcessBuilder` argument lists, stdin text input, timeout
  handling, temporary output cleanup, and WAV validation.
- React requests TTS after a successful voice response and plays the returned
  object URL.
- TTS is intentionally separate from `/api/voice/conversation`; TTS failure
  does not invalidate the STT/Qwen voice response.
- Browser Audio playback and object URL release were verified with a live
  local WAV response.

### Phase 6 — Complete voice loop

- Added explicit frontend voice states:
  - `IDLE`
  - `RECORDING`
  - `PROCESSING`
  - `SPEAKING`
  - `CORRECTION_PRACTICE`
  - `ERROR`
- Recording uses refs for the recorder, stream, chunks/recording metadata,
  and microphone-start cancellation.
- Start changes the button to `Stop Recording`; stop calls
  `MediaRecorder.stop()` exactly once.
- `ondataavailable` collects non-empty chunks.
- `onstop` creates one final Blob, rejects empty audio, releases microphone
  tracks, and submits exactly one voice request.
- Stable session ID is reused for all voice turns in the component.
- TTS requests have IDs and `AbortController` support to prevent stale
  playback. Audio objects are paused/cleared during cancellation and every
  object URL is revoked.
- Text response is preserved when TTS fails.
- Component unmount cleanup releases tracks and cancels audio/request work.
- Text chat was not routed through the voice session or TTS path.

## Important React StrictMode root cause and fix

The real browser issue was in the React lifecycle, not Chrome permissions or
MediaRecorder:

1. `main.jsx` renders `<App />` inside React `StrictMode`.
2. In development, StrictMode replays the mount effect.
3. The cleanup set `mountedRef.current = false`.
4. The replayed effect setup did not restore it to `true`.
5. `getUserMedia()` resolved successfully, but App.jsx evaluated the valid
   stream as stale because `mountedRef.current` was still false.
6. The stream was stopped before `MediaRecorder` construction.
7. No recorder events, Blob, or `/api/voice/conversation` request followed.

The fix was to set `mountedRef.current = true` at effect setup before returning
the cleanup. Do not remove this reset or reintroduce a one-time mounted flag
that is incompatible with StrictMode effect replay.

Temporary `[VoiceDebug]` logging currently traces:

- recording start and microphone request/resolution
- MediaRecorder setup, MIME type, construction, start, and state
- `ondataavailable` chunk size/type
- stop invocation and recorder states
- `onstop`, chunk count, and final Blob size/type
- voice request start/completion/response
- TTS start/completion/failure

Keep or remove this logging only deliberately; if removing it, preserve the
underlying lifecycle behavior and run the browser verification again.

## Browser evidence

Direct browser tests proved:

- `navigator.mediaDevices.getUserMedia({audio:true})` works and returns a
  `MediaStream`.
- Direct MediaRecorder test works:
  - recorder starts
  - `dataavailable` reports approximately 80,160 bytes
  - MIME is `audio/webm;codecs=opus`
  - recorder stops successfully
  - non-empty audio is produced

After the StrictMode fix, the application trace proved:

- `getUserMedia` resolved and stream was accepted.
- MediaRecorder was created with `audio/webm;codecs=opus`.
- `recorder.start()` changed state to `recording`.
- The UI displayed `⏹ Stop Recording`.
- `ondataavailable` produced a non-empty chunk.
- `onstop` fired after `stop()`.
- A non-empty final Blob was created.
- Exactly one `POST /api/voice/conversation` was issued for the recording.

The browser microphone API and application MediaRecorder path are therefore
verified. A complete successful STT -> Qwen -> Piper run requires the STT
service blocker below to be resolved.

## Current blocker: faster-whisper startup/model loading

The current blocker is the local Python STT service startup, not the React
recording implementation.

Starting:

```powershell
cd stt
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
```

currently fails while faster-whisper attempts model metadata/model loading
from Hugging Face with:

```text
[WinError 10054] An existing connection was forcibly closed by the remote host
```

The service exits before it can answer `/health` or `/transcribe`, so the live
browser voice request currently returns HTTP 503:

`Speech recognition service is unavailable. Please make sure the local STT
service is running.`

Do not misdiagnose this as a microphone or MediaRecorder failure. Do not
change backend/STT/Ollama/Piper while investigating a React-only task unless
the user explicitly requests it. The repository previously had a usable
ModelScope-cached `small` model, but the current startup path still attempted
the Hugging Face lookup and must be investigated separately.

## Latest test results

Latest verified commands:

Frontend:

```powershell
cd frontend
npm run build
```

Result: passed.

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Result: 38 tests passed, 0 failures, 0 errors, 0 skipped.

STT:

```powershell
cd stt
.\.venv\Scripts\python.exe -m pytest -q
```

Result: 7 passed, 0 failed, 0 errors, 0 skipped. There is one existing
Starlette/httpx deprecation warning.

The STT unit tests use a fake model and do not prove that the production
faster-whisper model can currently start.

## Architectural decisions to preserve

- Keep the voice endpoint and TTS endpoint separate.
- Keep text mode on `/api/conversation/message`.
- Keep sessions temporary and in memory; do not add SQLite yet.
- Keep Piper local and invoked through the Spring adapter.
- Keep Ollama behind Spring Boot; React never calls Ollama directly.
- Keep correction-practice responses concise while retaining structured
  grammar data in the API response.
- Submit one completed recording exactly once.
- Reuse one session ID for a voice conversation and generate a new one for a
  new conversation.
- Release microphone tracks, audio objects, temporary files, and object URLs.
- Prefer small, testable changes over rewrites or broad redesigns.

## Do not change unnecessarily

Do not rewrite working Phase 1–5 functionality, move Piper into the voice
endpoint, route text mode through voice sessions, add cloud dependencies,
commit model files, add authentication/analytics/persistence, or start Phase
7. Do not weaken existing tests or remove structured error handling.

Before any change, inspect the relevant source and documentation, preserve
unrelated user work, run the smallest relevant validation, then run the
frontend build plus backend/STT regression suites when the voice path is
affected. Do not commit unless explicitly requested.

