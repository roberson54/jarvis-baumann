package com.baumann.jarvis.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baumann.jarvis.data.local.JarvisDatabase
import com.baumann.jarvis.data.local.SecureKeyStore
import com.baumann.jarvis.data.remote.AnthropicClient
import com.baumann.jarvis.data.repository.ConversationRepositoryImpl
import com.baumann.jarvis.domain.model.ConversationMessage
import com.baumann.jarvis.domain.model.JarvisState
import com.baumann.jarvis.domain.model.Role
import com.baumann.jarvis.domain.repository.ConversationRepository
import com.baumann.jarvis.services.AIService
import com.baumann.jarvis.services.AndroidVoiceService
import com.baumann.jarvis.services.VoiceListener
import com.baumann.jarvis.services.VoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val state: JarvisState = JarvisState.IDLE,
    val heard: String = "",
    val response: String = "",
    val hasApiKey: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val keyStore = SecureKeyStore(app)
    private val repository: ConversationRepository =
        ConversationRepositoryImpl(JarvisDatabase.get(app).conversationDao())
    private val ai = AIService(AnthropicClient(), keyStore)
    private val voice: VoiceService = AndroidVoiceService(app)

    private val _ui = MutableStateFlow(HomeUiState(hasApiKey = keyStore.getApiKey() != null))
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    val history: StateFlow<List<ConversationMessage>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        voice.setListener(object : VoiceListener {
            override fun onListeningStarted() {
                _ui.update { it.copy(state = JarvisState.LISTENING) }
            }

            override fun onResult(text: String) {
                handleUserText(text)
            }

            override fun onError(message: String) {
                _ui.update { it.copy(state = JarvisState.ERROR, response = message) }
            }

            override fun onSpeakingStarted() {
                _ui.update { it.copy(state = JarvisState.SPEAKING) }
            }

            override fun onSpeakingDone() {
                _ui.update {
                    if (it.state == JarvisState.SPEAKING) it.copy(state = JarvisState.IDLE) else it
                }
            }
        })
    }

    fun onMicClick() {
        when (_ui.value.state) {
            JarvisState.LISTENING -> voice.stopListening()
            JarvisState.SPEAKING -> {
                voice.stopSpeaking()
                _ui.update { it.copy(state = JarvisState.IDLE) }
            }
            JarvisState.PROCESSING -> Unit
            JarvisState.IDLE, JarvisState.ERROR -> {
                if (!_ui.value.hasApiKey) {
                    _ui.update {
                        it.copy(
                            state = JarvisState.ERROR,
                            response = "Sr. Baumann, configure primeiro a chave da API."
                        )
                    }
                    return
                }
                _ui.update { it.copy(state = JarvisState.LISTENING, heard = "", response = "") }
                voice.startListening()
            }
        }
    }

    fun onPermissionDenied() {
        _ui.update {
            it.copy(
                state = JarvisState.ERROR,
                response = "Sr. Baumann, preciso da permissão do microfone para ouvi-lo."
            )
        }
    }

    fun saveApiKey(key: String) {
        keyStore.saveApiKey(key)
        _ui.update { it.copy(hasApiKey = keyStore.getApiKey() != null) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }

    private fun handleUserText(text: String) {
        viewModelScope.launch {
            _ui.update { it.copy(state = JarvisState.PROCESSING, heard = text, response = "") }
            repository.add(Role.USER, text)
            val result = ai.reply(repository.recent(HISTORY_WINDOW))
            result.onSuccess { reply ->
                repository.add(Role.ASSISTANT, reply)
                _ui.update { it.copy(state = JarvisState.SPEAKING, response = reply) }
                voice.speak(reply)
            }.onFailure { error ->
                _ui.update {
                    it.copy(
                        state = JarvisState.ERROR,
                        response = error.message ?: "Sr. Baumann, ocorreu um erro."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }

    private companion object {
        const val HISTORY_WINDOW = 20
    }
}
