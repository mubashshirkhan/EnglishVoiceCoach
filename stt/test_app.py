from dataclasses import dataclass

from fastapi.testclient import TestClient

from app import create_app


@dataclass
class FakeInfo:
    language: str = "en"
    duration: float = 2.4


@dataclass
class FakeSegment:
    text: str


class FakeModel:
    def __init__(self, text: str = "Hello, how are you?"):
        self.text = text

    def transcribe(self, path: str, **kwargs):
        return iter([FakeSegment(self.text)]), FakeInfo()


def test_health_endpoint():
    with TestClient(create_app(FakeModel())) as client:
        response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "model": "small"}


def test_transcribe_returns_structured_response():
    with TestClient(create_app(FakeModel())) as client:
        response = client.post(
            "/transcribe",
            files={"audio": ("speech.webm", b"fake audio", "audio/webm")},
        )

    assert response.status_code == 200
    assert response.json()["text"] == "Hello, how are you?"
    assert response.json()["language"] == "en"
    assert response.json()["duration"] == 2.4


def test_transcribe_accepts_webm_codec_parameter():
    with TestClient(create_app(FakeModel())) as client:
        response = client.post(
            "/transcribe",
            files={"audio": ("speech.webm", b"fake audio", "audio/webm;codecs=opus")},
        )

    assert response.status_code == 200


def test_missing_audio_is_rejected():
    with TestClient(create_app(FakeModel())) as client:
        response = client.post("/transcribe")

    assert response.status_code == 422


def test_empty_audio_is_rejected():
    with TestClient(create_app(FakeModel())) as client:
        response = client.post(
            "/transcribe",
            files={"audio": ("empty.webm", b"", "audio/webm")},
        )

    assert response.status_code == 400
    assert response.json()["detail"] == "Audio file is empty."


def test_unsupported_audio_is_rejected():
    with TestClient(create_app(FakeModel())) as client:
        response = client.post(
            "/transcribe",
            files={"audio": ("notes.txt", b"text", "text/plain")},
        )

    assert response.status_code == 415


def test_empty_transcription_is_rejected():
    with TestClient(create_app(FakeModel(text=""))) as client:
        response = client.post(
            "/transcribe",
            files={"audio": ("speech.webm", b"fake audio", "audio/webm")},
        )

    assert response.status_code == 422
    assert response.json()["detail"] == "No speech was detected. Please try again."
