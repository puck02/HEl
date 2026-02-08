package com.heldairy.feature.insights

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.DoctorReportData
import com.heldairy.core.data.DoctorReportRepository
import com.heldairy.core.data.InsightLocalSummary
import com.heldairy.core.data.InsightRepository
import com.heldairy.core.data.WeeklyInsightCoordinator
import com.heldairy.core.data.WeeklyInsightResult
import com.heldairy.core.data.WeeklyInsightStatus
import com.heldairy.core.pdf.ImprovedDoctorReportPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class InsightWindowType { Seven, Thirty }

data class WeeklyInsightUi(
    val status: WeeklyInsightStatus = WeeklyInsightStatus.Pending,
    val result: WeeklyInsightResult? = null
)

data class InsightsUiState(
    val isLoading: Boolean = true,
    val selectedWindow: InsightWindowType = InsightWindowType.Seven,
    val summary: InsightLocalSummary? = null,
    val weeklyInsight: WeeklyInsightUi = WeeklyInsightUi(),
    val error: String? = null,
    val isGeneratingPreview: Boolean = false,
    val previewPdfFile: File? = null,
    val previewError: String? = null,
    val reportStartDate: LocalDate? = null,
    val reportEndDate: LocalDate? = null
)

class InsightsViewModel(
    private val context: Context,
    private val insightRepository: InsightRepository,
    private val weeklyInsightCoordinator: WeeklyInsightCoordinator,
    private val doctorReportRepository: DoctorReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    init {
        // 先快速加载本地统计，延迟加载周洞察
        refreshData()
    }

    fun selectWindow(type: InsightWindowType) {
        _uiState.update { it.copy(selectedWindow = type) }
    }

    /**
     * 刷新每周洞察（自动判断是否需要生成）
     * - 周日首次打开：自动调用LLM生成本周洞察
     * - 非周日：显示上周的洞察
     * - 数据为空/失败：自动重新生成
     */
    fun refreshWeekly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val weekly = weeklyInsightCoordinator.getWeeklyInsight()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    weeklyInsight = WeeklyInsightUi(status = weekly.status, result = weekly)
                )
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                // 先加载本地统计，让UI快速显示
                val summary = insightRepository.buildLocalSummary()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary
                    )
                }
                
                // 延迟加载周洞察（不阻塞UI）
                launch {
                    val weekly = weeklyInsightCoordinator.getWeeklyInsight()
                    _uiState.update {
                        it.copy(weeklyInsight = WeeklyInsightUi(status = weekly.status, result = weekly))
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, error = throwable.message) }
            }
        }
    }

    /**
     * 生成PDF预览
     */
    fun generatePreview() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isGeneratingPreview = true,
                    previewError = null,
                    previewPdfFile = null
                ) 
            }
            
            runCatching {
                val state = _uiState.value
                val reportData = if (state.reportStartDate != null && state.reportEndDate != null) {
                    doctorReportRepository.generateReportDataWithDateRange(
                        state.reportStartDate,
                        state.reportEndDate
                    )
                } else {
                    val timeWindow = when (state.selectedWindow) {
                        InsightWindowType.Seven -> 7
                        InsightWindowType.Thirty -> 30
                    }
                    doctorReportRepository.generateReportData(timeWindow)
                }
                
                // 检查协程是否被取消
                ensureActive()
                
                // 生成PDF到临时文件
                withContext(Dispatchers.IO) {
                    val generator = ImprovedDoctorReportPdfGenerator(context, reportData)
                    generator.generateToTempFile()
                }
            }.onSuccess { pdfFile ->
                _uiState.update { 
                    it.copy(
                        isGeneratingPreview = false,
                        previewPdfFile = pdfFile
                    ) 
                }
            }.onFailure { throwable ->
                _uiState.update { 
                    it.copy(
                        isGeneratingPreview = false,
                        previewError = throwable.message ?: "生成预览失败"
                    ) 
                }
            }
        }
    }

    /**
     * 创建保存PDF文件的Intent
     */
    fun createSavePdfIntent(tempFile: File): Intent {
        currentTempFile = tempFile
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val fileName = "健康报表_${LocalDate.now().format(dateFormatter)}.pdf"
        
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }

    /**
     * 创建分享PDF文件的Intent
     */
    fun createShareIntent(pdfFile: File): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "健康报表")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.let { Intent.createChooser(it, "分享报表") }
        } catch (e: Exception) {
            _uiState.update { it.copy(previewError = "分享失败: ${e.message}") }
            null
        }
    }

    /**
     * 完成文件保存（从Activity的onActivityResult调用）
     */
    fun completeSave(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            currentTempFile?.let { tempFile ->
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }.onSuccess {
                    // 清理临时文件
                    tempFile.delete()
                    _uiState.update { it.copy(previewPdfFile = null) }
                }.onFailure { throwable ->
                    _uiState.update { it.copy(previewError = "保存失败: ${throwable.message}") }
                }
            }
            currentTempFile = null
        }
    }

    /**
     * 关闭预览，清理临时文件
     */
    fun closePreview() {
        _uiState.value.previewPdfFile?.delete()
        _uiState.update { 
            it.copy(
                previewPdfFile = null,
                previewError = null
            ) 
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理临时PDF文件
        _uiState.value.previewPdfFile?.delete()
        currentTempFile?.delete()
    }

    fun clearReportStatus() {
        _uiState.update { 
            it.copy(
                previewError = null
            ) 
        }
    }

    fun setReportDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        _uiState.update { 
            it.copy(
                reportStartDate = startDate,
                reportEndDate = endDate
            ) 
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val container = app.appContainer
                InsightsViewModel(
                    context = app.applicationContext,
                    insightRepository = container.insightRepository,
                    weeklyInsightCoordinator = container.weeklyInsightCoordinator,
                    doctorReportRepository = container.doctorReportRepository
                )
            }
        }
    }

    private var currentTempFile: File? = null
}
