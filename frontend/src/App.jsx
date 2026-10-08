import { useEffect, useRef, useState } from 'react'

const VOICE_STATES = {
  IDLE: 'IDLE',
  RECORDING: 'RECORDING',
  PROCESSING: 'PROCESSING',
  SPEAKING: 'SPEAKING',
  CORRECTION_PRACTICE: 'CORRECTION_PRACTICE',
  ERROR: 'ERROR',
}

function voiceLog(message, details = {}) {
  console.log(`[VoiceDebug] ${message}`, details)
}

function createSessionId() {
  return globalThis.crypto?.randomUUID?.() || `voice-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function App() {
  const [backendStatus, setBackendStatus] = useState('Checking...')
  const [message, setMessage] = useState('')
  const [conversation, setConversation] = useState([])
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState('')
  const [voiceState, setVoiceState] = useState(VOICE_STATES.IDLE)
  const [transcript, setTranscript] = useState('')
  const [sessionState, setSessionState] = useState('NORMAL_CONVERSATION')
  const [voiceSessionId, setVoiceSessionId] = useState(createSessionId)

  const recordingRef = useRef(null)
  const microphoneStartRef = useRef(null)
  const voiceRequestRef = useRef(0)
  const audioRef = useRef(null)
  const ttsRequestRef = useRef({ id: 0, controller: null })
  const mountedRef = useRef(true)

  useEffect(() => {
    fetch('/api/health')
      .then((response) => {
        if (!response.ok) {
          throw new Error('Health check failed')
        }
        return response.json()
      })
      .then(() => setBackendStatus('Connected'))
      .catch(() => setBackendStatus('Disconnected'))
  }, [])

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
      const recording = recordingRef.current
      if (microphoneStartRef.current) {
        microphoneStartRef.current.cancelled = true
        microphoneStartRef.current = null
      }
      recording?.stream.getTracks().forEach((track) => track.stop())
      if (recording?.recorder && recording.recorder.state === 'recording') {
        recording.recorder.onstop = null
        recording.recorder.stop()
      }
      cancelSpeech()
    }
  }, [])

  async function sendConversationMessage(rawMessage) {
    const trimmedMessage = rawMessage.trim()
    if (!trimmedMessage || isSending) {
      return
    }
    setError('')
    setIsSending(true)
    setConversation((current) => [...current, { role: 'You', text: trimmedMessage }])

    try {
      const response = await fetch('/api/conversation/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: trimmedMessage }),
      })
      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.message || 'The message could not be sent.')
      }
      setConversation((current) => [...current, { role: 'AI', response: data }])
    } catch (sendError) {
      setError(sendError.message || 'The local AI service is unavailable.')
    } finally {
      setIsSending(false)
    }
  }

  async function sendMessage(event) {
    event.preventDefault()
    if (!message.trim() || isSending) {
      return
    }
    const submittedMessage = message
    setMessage('')
    await sendConversationMessage(submittedMessage)
  }

  function canStartRecording() {
    return voiceState === VOICE_STATES.IDLE
      || voiceState === VOICE_STATES.CORRECTION_PRACTICE
      || voiceState === VOICE_STATES.ERROR
  }

  async function startRecording() {
    voiceLog('startRecording invoked', { voiceState, hasRecording: Boolean(recordingRef.current) })
    if (!canStartRecording() || isSending || recordingRef.current) {
      voiceLog('startRecording ignored', { voiceState, isSending, hasRecording: Boolean(recordingRef.current) })
      return
    }
    setError('')
    setTranscript('')
    cancelSpeech()

    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      showVoiceError('Audio recording is not supported by this browser.')
      return
    }

    const startAttempt = { cancelled: false }
    microphoneStartRef.current = startAttempt
    setVoiceState(VOICE_STATES.RECORDING)
    voiceLog('getUserMedia requested', { state: VOICE_STATES.RECORDING })
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      voiceLog('getUserMedia resolved', {
        cancelled: startAttempt.cancelled,
        tracks: stream.getTracks().length,
      })
      if (!mountedRef.current || startAttempt.cancelled) {
        stream.getTracks().forEach((track) => track.stop())
        if (mountedRef.current) {
          setVoiceState(VOICE_STATES.IDLE)
        }
        return
      }
      voiceLog('getUserMedia stream accepted')
      voiceLog('MediaRecorder setup started')
      const mimeType = [
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/ogg;codecs=opus',
      ].find((type) => MediaRecorder.isTypeSupported?.(type))
      voiceLog('MediaRecorder MIME selected', { mimeType })
      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream)
      voiceLog('MediaRecorder created', { requestedMimeType: mimeType, actualMimeType: recorder.mimeType, state: recorder.state })
      const recording = {
        id: Symbol('recording'),
        recorder,
        stream,
        chunks: [],
        submitted: false,
        stopping: false,
        failed: false,
      }
      recordingRef.current = recording
      microphoneStartRef.current = null
      recorder.ondataavailable = (event) => {
        voiceLog('ondataavailable fired', { size: event.data.size, type: event.data.type })
        if (event.data.size > 0) {
          recording.chunks.push(event.data)
        }
      }
      recorder.onerror = () => {
        voiceLog('MediaRecorder error', { state: recorder.state })
        recording.failed = true
        finishRecording(recording)
        showVoiceError('Microphone recording failed. Please try again.')
      }
      recorder.onstop = () => {
        voiceLog('onstop fired', { chunkCount: recording.chunks.length, state: recorder.state })
        if (recording.submitted || recording.failed) {
          voiceLog('onstop ignored', { submitted: recording.submitted, failed: recording.failed })
          return
        }
        recording.submitted = true
        finishRecording(recording)
        const blob = new Blob(recording.chunks, { type: recorder.mimeType || 'audio/webm' })
        voiceLog('final Blob created', { size: blob.size, type: blob.type, chunkCount: recording.chunks.length })
        if (blob.size === 0) {
          showVoiceError("I didn't catch that. Please try again.")
          return
        }
        transcribeRecording(blob)
      }
      try {
        voiceLog('recorder.start() called', { stateBefore: recorder.state })
        recorder.start()
        voiceLog('recorder.state after start', { state: recorder.state })
      } catch (startError) {
        recording.failed = true
        finishRecording(recording)
        throw startError
      }
    } catch (recordingError) {
      if (microphoneStartRef.current === startAttempt) {
        microphoneStartRef.current = null
      }
      if (startAttempt.cancelled) {
        voiceLog('recording start cancelled', { error: recordingError.name })
        return
      }
      voiceLog('recording initialization failed', { name: recordingError.name, message: recordingError.message })
      showVoiceError(recordingError.name === 'NotAllowedError'
        ? 'Microphone permission was denied. Please allow microphone access and try again.'
        : 'Could not start microphone recording.')
    }
  }

  function stopRecording() {
    voiceLog('stopRecording invoked', {
      hasRecording: Boolean(recordingRef.current),
      hasPendingStart: Boolean(microphoneStartRef.current),
      state: recordingRef.current?.recorder?.state,
    })
    const recording = recordingRef.current
    if (!recording && microphoneStartRef.current) {
      microphoneStartRef.current.cancelled = true
      microphoneStartRef.current = null
      setVoiceState(VOICE_STATES.IDLE)
      voiceLog('pending recording start cancelled')
      return
    }
    if (!recording || recording.stopping || recording.recorder.state !== 'recording') {
      return
    }
    recording.stopping = true
    setVoiceState(VOICE_STATES.PROCESSING)
    try {
      voiceLog('recorder.stop() called', { stateBefore: recording.recorder.state })
      recording.recorder.stop()
      voiceLog('recorder state after stop()', { state: recording.recorder.state })
    } catch (stopError) {
      recording.failed = true
      finishRecording(recording)
      showVoiceError('Could not stop microphone recording. Please try again.')
    }
  }

  function finishRecording(recording) {
    recording.stream.getTracks().forEach((track) => track.stop())
    if (recordingRef.current === recording) {
      recordingRef.current = null
    }
  }

  async function transcribeRecording(blob) {
    const requestId = voiceRequestRef.current + 1
    voiceRequestRef.current = requestId
    const formData = new FormData()
    const extension = blob.type.includes('ogg') ? 'ogg' : 'webm'
    formData.append('audio', blob, `recording.${extension}`)
    formData.append('sessionId', voiceSessionId)
    voiceLog('voice request started', { requestId, size: blob.size, type: blob.type, sessionId: voiceSessionId })
    try {
      const response = await fetch('/api/voice/conversation', { method: 'POST', body: formData })
      voiceLog('voice request completed', { requestId, status: response.status })
      const data = await response.json().catch(() => ({}))
      voiceLog('voice response received', { requestId, ok: response.ok, state: data.state, transcript: data.transcript })
      if (!response.ok) {
        throw new Error(data.message || 'Voice conversation failed.')
      }
      if (!mountedRef.current || requestId !== voiceRequestRef.current) {
        return
      }
      setTranscript(data.transcript)
      setSessionState(data.state)
      setConversation((current) => [
        ...current,
        { role: 'You', text: data.transcript },
        {
          role: 'AI',
          response: data.grammar || {
            errors: [],
            grammarCorrect: true,
            naturalnessSuggestion: null,
            conversationResponse: data.conversationResponse,
          },
          voiceState: data.state,
        },
      ])
      await speakResponse(data.conversationResponse, data.state)
    } catch (voiceError) {
      showVoiceError(voiceError.message || 'Voice conversation failed. Please try again.')
    }
  }

  async function speakResponse(text, backendState) {
    if (!text?.trim()) {
      setVoiceState(backendState === 'CORRECTION_PRACTICE'
        ? VOICE_STATES.CORRECTION_PRACTICE
        : VOICE_STATES.IDLE)
      return
    }
    const requestId = ttsRequestRef.current.id + 1
    const controller = new AbortController()
    ttsRequestRef.current = { id: requestId, controller }
    setVoiceState(VOICE_STATES.SPEAKING)
    voiceLog('TTS started', { requestId, text })
    try {
      const response = await fetch('/api/tts/speak', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text }),
        signal: controller.signal,
      })
      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.message || 'Voice playback is unavailable.')
      }
      const audioUrl = URL.createObjectURL(await response.blob())
      try {
        if (requestId !== ttsRequestRef.current.id) {
          return
        }
        const audio = new Audio(audioUrl)
        audioRef.current = audio
        await new Promise((resolve, reject) => {
          audio.addEventListener('ended', resolve, { once: true })
          audio.addEventListener('error', () => reject(new Error('Voice playback failed.')), { once: true })
          audio.play().catch(reject)
        })
      } finally {
        URL.revokeObjectURL(audioUrl)
        if (audioRef.current) {
          audioRef.current = null
        }
      }
      if (requestId === ttsRequestRef.current.id && mountedRef.current) {
        voiceLog('TTS completed', { requestId })
        setVoiceState(backendState === 'CORRECTION_PRACTICE'
          ? VOICE_STATES.CORRECTION_PRACTICE
          : VOICE_STATES.IDLE)
      }
    } catch (speechError) {
      if (speechError.name === 'AbortError' || requestId !== ttsRequestRef.current.id) {
        return
      }
      voiceLog('TTS failed', { requestId, name: speechError.name, message: speechError.message })
      showVoiceError(speechError.message || 'Voice playback failed. You can record another message.')
    } finally {
      if (requestId === ttsRequestRef.current.id) {
        ttsRequestRef.current.controller = null
      }
    }
  }

  function cancelSpeech() {
    ttsRequestRef.current.id += 1
    ttsRequestRef.current.controller?.abort()
    ttsRequestRef.current.controller = null
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.src = ''
      audioRef.current = null
    }
  }

  function showVoiceError(messageText) {
    if (!mountedRef.current) {
      return
    }
    setError(messageText)
    setVoiceState(VOICE_STATES.ERROR)
  }

  const isBusy = voiceState === VOICE_STATES.RECORDING
    || voiceState === VOICE_STATES.PROCESSING
    || voiceState === VOICE_STATES.SPEAKING

  return (
    <main className="app-shell">
      <section className="coach-card">
        <header className="hero">
          <p className="eyebrow">English practice, made simple</p>
          <h1>English Voice Coach</h1>
          <p className="subtitle">Practice English through natural conversation.</p>
          <p className={`status status-${backendStatus.toLowerCase()}`}>
            Backend: {backendStatus}
          </p>
        </header>

        <section className="conversation" aria-label="Conversation">
          {conversation.length === 0 ? (
            <p className="empty-state">Your conversation will appear here.</p>
          ) : (
            conversation.map((item, index) => (
              <ConversationMessage item={item} key={`${item.role}-${index}`} />
            ))
          )}
          {isSending && <p className="loading-state">Thinking...</p>}
          {error && <p className="error-state" role="alert">❌ {error}</p>}
        </section>

        <form className="composer" onSubmit={sendMessage}>
          <label htmlFor="message">Write a message to begin</label>
          <div className="composer-row">
            <input
              id="message"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="Type your English message..."
              disabled={isSending || isBusy}
            />
            <button className="send-button" type="submit" disabled={isSending || isBusy || !message.trim()}>
              {isSending ? 'Sending...' : 'Send'}
            </button>
          </div>
          <button
            className={`microphone-button ${voiceState === VOICE_STATES.RECORDING ? 'recording-button' : ''}`}
            type="button"
            onClick={voiceState === VOICE_STATES.RECORDING ? stopRecording : startRecording}
            disabled={isSending || voiceState === VOICE_STATES.PROCESSING || voiceState === VOICE_STATES.SPEAKING}
          >
            {voiceState === VOICE_STATES.RECORDING && '⏹ Stop Recording'}
            {voiceState === VOICE_STATES.PROCESSING && '⏳ Processing...'}
            {voiceState === VOICE_STATES.SPEAKING && '🔊 Speaking...'}
            {voiceState === VOICE_STATES.CORRECTION_PRACTICE && '✏️ Please repeat the corrected sentence'}
            {(voiceState === VOICE_STATES.IDLE || voiceState === VOICE_STATES.ERROR) && '🎤 Start Recording'}
          </button>
          {transcript && (
            <section className="transcript" aria-label="Transcript">
              <span>Transcript</span>
              <p>“{transcript}”</p>
            </section>
          )}
        </form>
      </section>
    </main>
  )
}

function ConversationMessage({ item }) {
  if (item.role === 'You') {
    return (
      <div className="message user-message">
        <span>You</span>
        <p>{item.text}</p>
      </div>
    )
  }

  const response = item.response
  return (
    <div className="ai-message">
      {item.voiceState && <span className="voice-state">{item.voiceState}</span>}
      {response.errors.length > 0 && (
        <section className="grammar-feedback" aria-label="Grammar correction">
          <span className="feedback-heading">Grammar correction</span>
          <strong>{response.correctedMessage}</strong>
          {response.errors.map((grammarError, index) => (
            <div className="grammar-error" key={`${grammarError.originalText}-${index}`}>
              <p><b>Why?</b> {grammarError.explanation}</p>
              <p><b>Grammar rule:</b> {grammarError.grammarRule}</p>
              <p><b>Example:</b> “{grammarError.example}”</p>
            </div>
          ))}
        </section>
      )}
      {response.grammarCorrect && response.naturalnessSuggestion && (
        <p className="naturalness-feedback">
          <b>Natural alternative:</b> {response.naturalnessSuggestion}
        </p>
      )}
      <div className="message ai-message-text">
        <span>AI</span>
        <p>{response.conversationResponse}</p>
      </div>
    </div>
  )
}

export default App
