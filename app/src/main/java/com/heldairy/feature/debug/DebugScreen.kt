package com.heldairy.feature.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.testing.TestDataGenerator
import com.heldairy.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebugUiState(
    val isGenerating: Boolean = false,
    val message: String? = null
)

class DebugViewModel(
    private val testDataGenerator: TestDataGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState

    fun generateYearData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            runCatching {
                testDataGenerator.generateYearOfData()
                _uiState.update { it.copy(isGenerating = false, message = "✅ 成功生成一年测试数据！") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isGenerating = false, message = "❌ 生成失败: ${throwable.message}") }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            runCatching {
                testDataGenerator.clearAllData()
                _uiState.update { it.copy(isGenerating = false, message = "✅ 已清除所有数据！") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isGenerating = false, message = "❌ 清除失败: ${throwable.message}") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val dao = app.appContainer.database.dailyReportDao()
                DebugViewModel(TestDataGenerator(dao))
            }
        }
    }
}

@Composable
fun DebugScreen(
    viewModel: DebugViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.M),
        verticalArrangement = Arrangement.spacedBy(Spacing.M),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛠️ 调试工具",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Spacing.M),
                verticalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                Text(
                    text = "测试数据生成",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "生成一年量的随机健康日记数据，用于测试报表功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = { viewModel.generateYearData() },
                    enabled = !state.isGenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isGenerating) "生成中..." else "生成一年测试数据")
                }

                Button(
                    onClick = { viewModel.clearAllData() },
                    enabled = !state.isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除所有数据")
                }
            }
        }

        state.message?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.startsWith("✅")) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.M),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.dismissMessage() }) {
                        Text("关闭")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "⚠️ 注意：此功能仅用于开发测试",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
