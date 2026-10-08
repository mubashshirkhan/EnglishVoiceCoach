# Local Speech-to-Text Service

This service is the Phase 3 local speech-to-text boundary. It loads one
`faster-whisper` model at startup, accepts a browser recording, and returns
transcribed text. It does not call Ollama and does not perform grammar
analysis.

## Setup

```powershell
cd stt
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
```

The verified machine-wide environment is Python 3.13.7. `faster-whisper`
was installed with:

```powershell
python -m pip install faster-whisper
```

## Configuration

- `WHISPER_MODEL=small` (default)
- `WHISPER_DEVICE=cpu` (default)
- `WHISPER_COMPUTE_TYPE=int8` (default)

The `small` model is the initial CPU choice for better accuracy while
remaining practical on the target 16 GB Intel machine. If startup or
inference is impractical, set `WHISPER_MODEL=base`.

The verification environment has not cached the model yet. Its first
download attempt was blocked when the Hugging Face connection was forcibly
closed; startup fails clearly rather than accepting requests without a model.

## Start

```powershell
cd stt
python -m uvicorn app:app --host 127.0.0.1 --port 8000
```

The first startup downloads the selected model if it is not cached. Model
loading happens once before requests are accepted.

## API

Health:

```http
GET http://127.0.0.1:8000/health
```

Transcription:

```http
POST http://127.0.0.1:8000/transcribe
Content-Type: multipart/form-data
```

The form field is `audio`. Browser recordings in WebM/Opus are supported;
Ogg, WAV, MP3, MP4/M4A, and AAC are also accepted when supplied with a
recognized media type or extension. The upload limit is 25 MB.

Example response:

```json
{
  "text": "Yesterday I went to college.",
  "language": "en",
  "duration": 3.42,
  "transcriptionDuration": 2.8
}
```

## Tests

```powershell
cd stt
python -m pytest
```

Tests use a fake model and do not download a Whisper model or perform
expensive inference.
