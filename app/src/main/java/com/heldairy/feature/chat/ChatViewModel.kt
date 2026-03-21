package com.heldairy.feature.chat

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.network.agent.AgentClientErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val responseTimeMs: Int? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val showProgressPanel: Boolean = false,
    val progressLogs: List<String> = emptyList()
)

class ChatViewModel(
    application: Application,
    private val agentClient: AgentClient?
) : AndroidViewModel(application) {

    private val fallbackChatError = "聊天请求失败，请稍后重试"

    private val isDebugBuild: Boolean =
        (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

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
            isLoading = true,
            showProgressPanel = true,
            progressLogs = listOf("开始处理请求...")
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

            agentClient.chatStream(
                message = text,
                sessionId = sessionId,
                onProgress = { event ->
                    appendProgress(formatProgress(event.stage, event.message, event.elapsedMs))
                }
            ).fold(
                onSuccess = { resp ->
                    sessionId = resp.sessionId
                    replaceLoading(
                        ChatMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            text = resp.answer,
                            isFromUser = false,
                            responseTimeMs = resp.responseTimeMs
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showProgressPanel = false,
                        progressLogs = emptyList()
                    )
                },
                onFailure = { err ->
                    val readableError = AgentClientErrorMapper.toUserMessage(err, fallbackChatError)
                    val detailMessage = err.message?.trim().orEmpty()
                    val errorType = err::class.java.simpleName
                    val rootCauseType = generateSequence(err) { it.cause }.last()::class.java.simpleName
                    Log.e(TAG, "chat_send_failed type=$errorType detail=$detailMessage", err)

                    val messageText = if (isDebugBuild) {
                        val detail = detailMessage.ifBlank { "<empty>" }
                        "发送失败：$readableError\n[DEBUG] type=$errorType cause=$rootCauseType detail=$detail"
                    } else if (readableError == fallbackChatError) {
                        "发送失败：$readableError（$rootCauseType）"
                    } else {
                        "发送失败：$readableError"
                    }

                    replaceLoading(
                        ChatMessage(
                            id = "err_${System.currentTimeMillis()}",
                            text = messageText,
                            isFromUser = false
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showProgressPanel = false
                    )
                }
            )
        }
    }

    private fun appendProgress(line: String) {
        val logs = (_uiState.value.progressLogs + line).takeLast(80)
        _uiState.value = _uiState.value.copy(progressLogs = logs)
    }

    private fun formatProgress(stage: String, message: String, elapsedMs: Int?): String {
        val elapsed = elapsedMs?.let { " (${it}ms)" } ?: ""
        return "[$stage] $message$elapsed"
    }

    private fun replaceLoading(msg: ChatMessage) {
        val msgs = _uiState.value.messages.toMutableList()
        val idx = msgs.indexOfLast { it.isLoading }
        if (idx >= 0) msgs[idx] = msg else msgs.add(msg)
        _uiState.value = _uiState.value.copy(messages = msgs)
    }

    companion object {
        private const val TAG = "ChatViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication
                ChatViewModel(app, app.appContainer.agentClient)
            }
        }
    }
}
