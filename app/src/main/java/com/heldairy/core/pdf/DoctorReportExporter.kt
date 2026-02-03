package com.heldairy.core.pdf

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import com.heldairy.core.data.DoctorReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

/**
 * PDF医生报表导出助手
 * 封装打印框架调用逻辑
 */
class DoctorReportExporter {

    /**
     * 生成并打印/保存PDF报表
     * @param activity Activity上下文（PrintManager要求）
     * @param reportData 报表数据
     * @return 操作是否启动成功
     */
    suspend fun exportToPdf(activity: Activity, reportData: DoctorReportData): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
            
            // 生成文件名
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val fileName = "健康报表_${reportData.reportDate.format(formatter)}"
            
            // 创建PrintDocumentAdapter
            val adapter = DoctorReportPdfGenerator(activity, reportData)
            
            // 启动打印任务（用户可选择保存为PDF或打印）
            printManager.print(fileName, adapter, PrintAttributes.Builder().build())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
