import os
import tempfile
import time
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Annotated, Any

from fastapi import FastAPI, File, HTTPException, UploadFile
from faster_whisper import WhisperModel

MAX_AUDIO_BYTES = 25 * 1024 * 1024
SUPPORTED_MEDIA_TYPES = {
    "audio/webm",
    "audio/ogg",
    "audio/wav",
    "audio/x-wav",
    "audio/mpeg",
    "audio/mp4",
    "audio/aac",
}
SUPPORTED_EXTENSIONS = {".webm", ".ogg", ".wav", ".mp3", ".mp4", ".m4a", ".aac"}


def model_name() -> str:
    return os.getenv("WHISPER_MODEL", "small")


def create_app(model: Any | None = None) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.model = model or WhisperModel(
            model_name(),
            device=os.getenv("WHISPER_DEVICE", "cpu"),
            compute_type=os.getenv("WHISPER_COMPUTE_TYPE", "int8"),
        )
        yield

    app = FastAPI(title="English Voice Coach Local STT", lifespan=lifespan)

    @app.get("/health")
    async def health() -> dict[str, str]:
        return {"status": "ok", "model": model_name()}

    @app.post("/transcribe")
    async def transcribe(audio: Annotated[UploadFile, File(...)]) -> dict[str, Any]:
        if not audio.filename:
            raise HTTPException(status_code=400, detail="Audio filename is required.")
        extension = Path(audio.filename).suffix.lower()
        media_type = (audio.content_type or "").split(";", 1)[0].lower()
        if media_type not in SUPPORTED_MEDIA_TYPES and extension not in SUPPORTED_EXTENSIONS:
            raise HTTPException(status_code=415, detail="Unsupported audio format.")

        contents = await audio.read(MAX_AUDIO_BYTES + 1)
        if not contents:
            raise HTTPException(status_code=400, detail="Audio file is empty.")
        if len(contents) > MAX_AUDIO_BYTES:
            raise HTTPException(status_code=413, detail="Audio file is too large.")

        started = time.perf_counter()
        temporary_path: str | None = None
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=extension or ".webm") as temporary_file:
                temporary_file.write(contents)
                temporary_path = temporary_file.name

            segments, info = app.state.model.transcribe(
                temporary_path,
                language="en",
                vad_filter=True,
            )
            segment_list = list(segments)
            text = " ".join(segment.text.strip() for segment in segment_list).strip()
            if not text:
                raise HTTPException(status_code=422, detail="No speech was detected. Please try again.")

            return {
                "text": text,
                "language": info.language or "en",
                "duration": round(float(info.duration), 2),
                "transcriptionDuration": round(time.perf_counter() - started, 2),
            }
        except HTTPException:
            raise
        except Exception as exception:
            raise HTTPException(
                status_code=500,
                detail="Transcription failed. Please try another recording.",
            ) from exception
        finally:
            if temporary_path:
                Path(temporary_path).unlink(missing_ok=True)

    return app


app = create_app()
