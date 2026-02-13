package com.heldairy.feature.insights.preview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heldairy.R
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF预览界面状态
 */
data class PdfPreviewUiState(
    val isLoading: Boolean = true,
    val pages: List<Bitmap> = emptyList(),
    val pageCount: Int = 0,
    val error: String? = null,
    val isSaving: Boolean = false
)

/**
 * PDF预览界面
 * 使用PdfRenderer渲染PDF页面为位图显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    pdfFile: File,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(PdfPreviewUiState()) }

    // 清理PDF渲染器资源，防止文件句柄泄漏
    DisposableEffect(pdfFile) {
        onDispose {
            // 释放bitmap内存
            uiState.pages.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    // 加载PDF页面
    LaunchedEffect(pdfFile) {
        uiState = uiState.copy(isLoading = true, error = null)
        runCatching {
            loadPdfPages(pdfFile)
        }.onSuccess { pages ->
            uiState = uiState.copy(
                isLoading = false,
                pages = pages,
                pageCount = pages.size
            )
        }.onFailure { throwable ->
            uiState = uiState.copy(
                isLoading = false,
                error = throwable.message ?: "加载PDF失败"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.pdf_preview_title))
                        if (uiState.pageCount > 0) {
                            Text(
                                stringResource(R.string.pdf_preview_page_count, uiState.pageCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.error == null) {
                PdfPreviewBottomBar(
                    onSave = onSave,
                    onShare = onShare,
                    onRegenerate = onRegenerate,
                    isSaving = uiState.isSaving
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error!!,
                        onRetry = { onRegenerate() }
                    )
                }
                else -> {
                    PdfPagesView(pages = uiState.pages)
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.M)
        ) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.pdf_preview_generating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.L),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.L),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.M)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    stringResource(R.string.pdf_preview_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
                Button(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf_preview_retry))
                }
            }
        }
    }
}

@Composable
private fun PdfPagesView(pages: List<Bitmap>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.M),
        verticalArrangement = Arrangement.spacedBy(Spacing.M)
    ) {
        items(
            count = pages.size,
            key = { index -> index }  // 使用索引作为key
        ) { index ->
            PdfPageItem(
                bitmap = pages[index],
                pageNumber = index + 1,
                totalPages = pages.size
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    bitmap: Bitmap,
    pageNumber: Int,
    totalPages: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(CornerRadius.Medium)
            ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 页码标签
            if (totalPages > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        stringResource(R.string.pdf_preview_page_indicator, pageNumber, totalPages),
                        modifier = Modifier.padding(horizontal = Spacing.M, vertical = Spacing.S),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            
            // PDF页面图像
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_page_description, pageNumber),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
private fun PdfPreviewBottomBar(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    isSaving: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S, vertical = Spacing.XS),
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 重新生成按钮 - 紧凑图标按钮
            OutlinedIconButton(
                onClick = onRegenerate,
                enabled = !isSaving,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.pdf_preview_regenerate),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 分享按钮 - 紧凑图标按钮
            OutlinedIconButton(
                onClick = onShare,
                enabled = !isSaving,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.pdf_preview_share),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 保存PDF按钮 - 占剩余宽度
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.pdf_preview_saving),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.pdf_preview_save),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * 从PDF文件加载所有页面为Bitmap
 */
private suspend fun loadPdfPages(pdfFile: File): List<Bitmap> = withContext(Dispatchers.IO) {
    val fileDescriptor = ParcelFileDescriptor.open(
        pdfFile,
        ParcelFileDescriptor.MODE_READ_ONLY
    )
    val renderer = PdfRenderer(fileDescriptor)
    val pages = mutableListOf<Bitmap>()

    try {
        for (pageIndex in 0 until renderer.pageCount) {
            val page = renderer.openPage(pageIndex)
            
            // 计算合适的渲染尺寸（保持A4比例，宽度适配屏幕）
            val renderWidth = 2400 // 高质量渲染（提升清晰度）
            val renderHeight = (page.height.toFloat() / page.width.toFloat() * renderWidth).toInt()
            
            val bitmap = Bitmap.createBitmap(
                renderWidth,
                renderHeight,
                Bitmap.Config.ARGB_8888
            )
            
            // 渲染页面到bitmap
            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            
            pages.add(bitmap)
            page.close()
        }
    } finally {
        renderer.close()
        fileDescriptor.close()
    }

    pages
}
