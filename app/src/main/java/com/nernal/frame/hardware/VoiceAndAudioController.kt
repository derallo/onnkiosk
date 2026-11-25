package com.nernal.frame.hardware

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure Android standard-library audio, chime, TTS, and speech recognition controller.
 * Keeps SpeechRecognizer interactions strictly on the Main Looper as required by Android.
 */
class VoiceAndAudioController(
    private val context: Context,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onSpeakingStarted: () -> Unit,
    private val onSpeakingFinished: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady = false
    private var pendingSpeakText: String? = null
    private var pendingSpeakDone: (() -> Unit)? = null
    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isListening = false
    private var activeUtteranceId: String? = null
    private var onTtsDoneCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun initTts() {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    engine.language = Locale.US
                    engine.setSpeechRate(1.0f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post { onSpeakingStarted() }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                if (utteranceId == null || utteranceId == activeUtteranceId) {
                                    activeUtteranceId = null
                                    val cb = onTtsDoneCallback
                                    onTtsDoneCallback = null
                                    onSpeakingFinished()
                                    cb?.invoke()
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            onDone(utteranceId)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            onDone(utteranceId)
                        }
                    })
                    ttsReady = true
                    val queued = pendingSpeakText
                    val queuedDone = pendingSpeakDone
                    pendingSpeakText = null
                    pendingSpeakDone = null
                    if (!queued.isNullOrBlank()) {
                        speak(queued, queuedDone)
                    }
                } else {
                    Log.w("NernalAudio", "TTS init failed status=$status")
                }
            }
        }
    }

    /**
     * Warm 4-note kitchen chime synthesized via AudioTrack (C5, E5, G5, C6) matching Pi's playKitchenChime().
     */
    fun playChime() {
        thread(isDaemon = true, name = "NernalChime") {
            runCatching {
                val sampleRate = 22_050
                val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                val stepSec = 0.16
                val noteDurationSec = 0.45
                val totalSamples = ((notes.size - 1) * stepSec + noteDurationSec) * sampleRate
                val pcm = ShortArray(totalSamples.toInt())

                for ((idx, freq) in notes.withIndex()) {
                    val startSample = (idx * stepSec * sampleRate).toInt()
                    val lenSamples = (noteDurationSec * sampleRate).toInt()
                    for (i in 0 until lenSamples) {
                        val pos = startSample + i
                        if (pos >= pcm.size) break
                        val t = i.toDouble() / sampleRate
                        val attack = (t / 0.025).coerceIn(0.0, 1.0)
                        val decay = Math.exp(-t * 6.5)
                        val sample = sin(2.0 * PI * freq * t) * attack * decay * 0.28
                        val mixed = pcm[pos] + (sample * Short.MAX_VALUE).toInt()
                        pcm[pos] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }

                val bufBytes = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(((totalSamples / sampleRate) * 1000).toLong() + 120L)
                runCatching { track.stop() }
                runCatching { track.release() }
            }.onFailure { e ->
                Log.w("NernalAudio", "Failed to play chime: ${e.message}")
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val clean = sanitizeForSpeech(text)
        if (clean.isBlank()) {
            onDone?.invoke()
            return
        }
        mainHandler.post {
            if (!ttsReady || tts == null) {
                pendingSpeakText = clean
                pendingSpeakDone = onDone
                if (tts == null) initTts()
                return@post
            }
            val utteranceId = UUID.randomUUID().toString()
            activeUtteranceId = utteranceId
            onTtsDoneCallback = onDone
            onSpeakingStarted()
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            pendingSpeakText = null
            pendingSpeakDone = null
            activeUtteranceId = null
            onTtsDoneCallback = null
            runCatching { tts?.stop() }
            onSpeakingFinished()
        }
    }

    fun startListening() {
        mainHandler.post {
            if (!hasRecordAudioPermission()) {
                onSpeechError("Microphone permission is needed to listen.")
                return@post
            }
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onSpeechError("On-device speech recognition is unavailable on this device.")
                return@post
            }
            stopSpeakingInternal()
            destroyRecognizerInternal()

            val recognizer = runCatching {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.getOrNull()
            if (recognizer == null) {
                onSpeechError("Could not start speech recognizer.")
                return@post
            }
            speechRecognizer = recognizer
            isListening = true

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (!partial.isNullOrBlank()) {
                        onPartialTranscript(partial)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    destroyRecognizerInternal()
                    if (text.isNotBlank()) {
                        onFinalTranscript(text)
                    } else {
                        onSpeechError("No speech detected.")
                    }
                }

                override fun onError(error: Int) {
                    isListening = false
                    destroyRecognizerInternal()
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that."
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout."
                        else -> "Speech recognition error ($error)."
                    }
                    onSpeechError(msg)
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1300L)
            }
            runCatching {
                recognizer.startListening(intent)
            }.onFailure { err ->
                isListening = false
                destroyRecognizerInternal()
                onSpeechError(err.message ?: "Unable to start listening.")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            destroyRecognizerInternal()
        }
    }

    fun release() {
        mainHandler.post {
            isListening = false
            destroyRecognizerInternal()
            stopSpeakingInternal()
            runCatching { tts?.shutdown() }
            tts = null
            ttsReady = false
        }
    }

    private fun stopSpeakingInternal() {
        pendingSpeakText = null
        pendingSpeakDone = null
        activeUtteranceId = null
        onTtsDoneCallback = null
        runCatching { tts?.stop() }
    }

    private fun destroyRecognizerInternal() {
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    companion object {
        fun sanitizeForSpeech(raw: String): String = raw
            .replace(Regex("""[*_`#]+"""), "")
            .replace(Regex("""https?://\S+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
