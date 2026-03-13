package com.heldairy.feature.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.R
import com.heldairy.feature.chat.ChatMessage
import com.heldairy.feature.chat.ChatUiState
import com.heldairy.feature.chat.ChatViewModel
import com.heldairy.ui.theme.BackgroundTheme
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.Spacing
import android.widget.Toast

@Composable
fun ChatRoute(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        uiState = uiState,
        onBack = onBack,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val listState = rememberLazyListState()
    val progressListState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.progressLogs.size) {
        if (uiState.progressLogs.isNotEmpty()) {
            progressListState.animateScrollToItem(uiState.progressLogs.size - 1)
        }
    }

    KittyBackground(backgroundRes = BackgroundTheme.HOME) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.default_avatar_kitty),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text("Kitty 健康管家 \uD83C\uDF80")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = Spacing.M, vertical = Spacing.M)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
                    ) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = onInputChanged,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("有什么想问的…", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(CornerRadius.Large),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                        IconButton(
                            onClick = onSendMessage,
                            enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (uiState.inputText.isNotBlank() && !uiState.isLoading)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "发送",
                                tint = if (uiState.inputText.isNotBlank() && !uiState.isLoading)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.messages.isEmpty()) {
                    ChatWelcome(modifier = Modifier.padding(innerPadding))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = Spacing.M, vertical = Spacing.S),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            MessageBubble(message = msg)
                        }
                    }
                }

                if (uiState.showProgressPanel) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.M, vertical = 110.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(CornerRadius.Medium)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.M)) {
                            Text(
                                text = "正在推理与工具调用...",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Spacing.XS))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(Spacing.S))
                            LazyColumn(
                                state = progressListState,
                                modifier = Modifier.height(140.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.progressLogs) { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatWelcome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.default_avatar_kitty),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(Spacing.M))
        Text(
            text = "你好，我是 Kitty 健康管家 \uD83C\uDF80",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.XS))
        Text(
            text = "可以问我关于健康、用药、数据分析等任何问题～",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copyText = remember(message.text) { message.text.trim() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Image(
                painter = painterResource(id = R.drawable.default_avatar_kitty),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(Spacing.XS))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = if (message.isFromUser) CornerRadius.Large else CornerRadius.Small,
                topEnd = if (message.isFromUser) CornerRadius.Small else CornerRadius.Large,
                bottomStart = CornerRadius.Large,
                bottomEnd = CornerRadius.Large,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pointerInput(copyText) {
                    detectTapGestures(
                        onLongPress = {
                            if (copyText.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(copyText))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
        ) {
            if (message.isLoading) {
                LoadingDots(
                    modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S)) {
                    if (!message.isFromUser && message.responseTimeMs != null) {
                        Text(
                            text = "响应时间 ${message.responseTimeMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isFromUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
