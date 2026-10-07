package com.baumann.jarvis.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Implementação com as APIs nativas: SpeechRecognizer (voz -> texto) e TextToSpeech (texto -> voz).
 * Deve ser criada na thread principal.
 */
class AndroidVoiceService(context: Context) : VoiceService {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var listener: VoiceListener? = null
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private var tts: TextToSpeech? = null
    private var ttsInitDone = false
    private var ttsReady = false
    private var pendingSpeech: String? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsInitDone = true
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("pt", "BR"))
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        main.post { listener?.onSpeakingStarted() }
                    }

                    override fun onDone(utteranceId: String?) {
                        main.post { listener?.onSpeakingDone() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post { listener?.onSpeakingDone() }
                    }
                })
            }
            pendingSpeech?.let {
                pendingSpeech = null
                speak(it)
            }
        }
    }

    override fun setListener(listener: VoiceListener?) {
        this.listener = listener
    }

    override fun isListening(): Boolean = listening

    override fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            listener?.onError(
                "Reconhecimento de voz indisponível. Verifique se o app Google (serviços de fala) está instalado e ativo."
            )
            return
        }
        stopSpeaking()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(recognitionListener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        listening = true
        recognizer?.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }

    override fun speak(text: String) {
        if (!ttsInitDone) {
            pendingSpeech = text
            return
        }
        if (!ttsReady) {
            // Sem voz em português: o texto continua visível na tela.
            listener?.onSpeakingDone()
            return
        }
        tts?.speak(text.take(MAX_TTS_CHARS), TextToSpeech.QUEUE_FLUSH, null, "jarvis-utterance")
    }

    override fun stopSpeaking() {
        tts?.stop()
    }

    override fun shutdown() {
        listening = false
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener?.onListeningStarted()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            listening = false
            listener?.onError(errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (text.isNullOrBlank()) {
                listener?.onError("Não consegui entender. Toque no microfone e tente de novo.")
            } else {
                listener?.onResult(text)
            }
        }
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Não ouvi nada. Toque no microfone e tente de novo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Sem permissão para usar o microfone."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "O reconhecimento de voz precisou de internet e não conseguiu conexão."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "O reconhecedor de voz está ocupado. Tente novamente."
        else -> "Erro no reconhecimento de voz (código $error)."
    }

    private companion object {
        const val MAX_TTS_CHARS = 3900
    }
}
