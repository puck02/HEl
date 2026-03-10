package com.heldairy.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.network.agent.AgentClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false
)

class ChatViewModel(
    application: Application,
    private val agentClient: AgentClient?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            text = text,
            isFromUser = true
        )
        val loadingMsg = ChatMessage(
            id = "loading",
            text = "",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + loadingMsg,
            inputText = "",
            isLoading = true
        )

        viewModelScope.launch {
            if (agentClient == null) {
                replaceLoading(
                    ChatMessage(
                        id = "err_${System.currentTimeMillis()}",
                        text = "Agent 未配置，请先在「设置 → 智能体」中配置服务器地址并登录。",
                        isFromUser = false
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            agentClient.chat(text, sessionId).fold(
                onSuccess = { resp ->
                    sessionId = resp.sessionId
                    replaceLoading(
                        ChatMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            text = resp.answer,
                            isFromUser = false
                        )
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { err ->
                    replaceLoading(
                        ChatMessage(
                            id = "err_${System.currentTimeMillis()}",
                            text = "发送失败：${err.message ?: "未知错误"}",
                            isFromUser = false
                        )
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    private fun replaceLoading(msg: ChatMessage) {
        val msgs = _uiState.value.messages.toMutableList()
        val idx = msgs.indexOfLast { it.isLoading }
        if (idx >= 0) msgs[idx] = msg else msgs.add(msg)
        _uiState.value = _uiState.value.copy(messages = msgs)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication
                ChatViewModel(app, app.appContainer.agentClient)
            }
        }
    }
}
