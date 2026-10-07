package com.baumann.jarvis.services

interface VoiceListener {
    fun onListeningStarted() {}
    fun onResult(text: String) {}
    fun onError(message: String) {}
    fun onSpeakingStarted() {}
    fun onSpeakingDone() {}
}

interface VoiceService {
    fun startListening()
    fun stopListening()
    fun speak(text: String)
    fun isListening(): Boolean

    fun stopSpeaking()
    fun setListener(listener: VoiceListener?)
    fun shutdown()
}
