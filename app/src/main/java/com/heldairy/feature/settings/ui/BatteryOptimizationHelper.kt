package com.heldairy.feature.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 电池优化白名单引导工具
 * 
 * 部分厂商（小米、华为、OPPO、vivo）会积极杀后台进程，
 * 需要引导用户关闭电池优化以确保提醒准时触发。
 */
object BatteryOptimizationHelper {

    /**
     * 检查应用是否已忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 打开电池优化设置页面
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 部分设备不支持此 Intent，打开应用详情页
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // 忽略
            }
        }
    }

    /**
     * 获取当前设备品牌特定的自启动设置提示
     */
    fun getManufacturerHint(): String? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi" -> "请在「设置 → 应用 → 自启动管理」中允许 HElDairy 自启动"
            "huawei", "honor" -> "请在「设置 → 电池 → 启动管理」中允许 HElDairy 自启动"
            "oppo", "realme", "oneplus" -> "请在「设置 → 电池 → 后台耗电管理」中允许 HElDairy"
            "vivo", "iqoo" -> "请在「设置 → 电池 → 后台高耗电」中允许 HElDairy"
            "samsung" -> "请在「设置 → 电池 → 后台使用限制」中解除 HElDairy 限制"
            "meizu" -> "请在「设置 → 电量管理 → 后台管理」中允许 HElDairy"
            else -> null
        }
    }
}
