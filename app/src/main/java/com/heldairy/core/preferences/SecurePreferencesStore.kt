package com.heldairy.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 安全的加密 SharedPreferences 存储
 * 
 * 使用 Jetpack Security 的 EncryptedSharedPreferences 保护敏感数据。
 * 数据在写入磁盘前自动加密，读取时自动解密。
 * 
 * 主要用于存储：
 * - DeepSeek API Key
 * - 其他需要保护的用户凭证
 */
class SecurePreferencesStore(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: Flow<String> = _apiKeyFlow.asStateFlow()

    /**
     * 获取存储的 API Key（加密读取）
     */
    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
    }

    /**
     * 保存 API Key（加密写入）
     * 
     * @param apiKey 待保存的密钥，传入空字符串将删除已存储的密钥
     */
    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            encryptedPrefs.edit().remove(KEY_API_KEY).apply()
        } else {
            encryptedPrefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        }
        _apiKeyFlow.value = getApiKey()
    }

    /**
     * 清除所有加密存储的数据
     * 
     * ⚠️ 谨慎使用：此操作会删除所有加密存储的内容
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        _apiKeyFlow.value = ""
    }

    companion object {
        private const val FILENAME = "heldairy_secure_prefs"
        private const val KEY_API_KEY = "deepseek_api_key_v2" // v2 表示加密版本
    }
}
