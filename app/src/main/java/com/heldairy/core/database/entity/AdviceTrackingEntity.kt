package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阶段2：建议追踪表 - 记录AI给出的建议及用户反馈
 */
@Entity(
    tableName = "advice_tracking",
    foreignKeys = [
        ForeignKey(
            entity = DailyAdviceEntity::class,
            parentColumns = ["entry_id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entry_id"]),
        Index(value = ["generated_date"]),
        Index(value = ["category"])
    ]
)
data class AdviceTrackingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entry_id")
    val entryId: Long,  // 关联的日报条目
    
    @ColumnInfo(name = "advice_text")
    val adviceText: String,  // 具体建议内容
    
    @ColumnInfo(name = "generated_date")
    val generatedDate: String,  // 生成日期 (ISO-8601格式)
    
    @ColumnInfo(name = "category")
    val category: String,  // 类别：sleep/exercise/diet/emotion/symptom/other
    
    @ColumnInfo(name = "source_field")
    val sourceField: String,  // 来源字段：observation/action/tomorrowFocus
    
    @ColumnInfo(name = "user_feedback")
    val userFeedback: String? = null,  // 用户反馈：helpful/not_helpful/executed/dismissed
    
    @ColumnInfo(name = "feedback_at")
    val feedbackAt: Long? = null,  // 反馈时间戳
    
    @ColumnInfo(name = "effectiveness_score")
    val effectivenessScore: Int? = null,  // 执行后效果评分 1-5
    
    @ColumnInfo(name = "execution_note")
    val executionNote: String? = null  // 执行备注
)

/**
 * 用户反馈枚举（在Kotlin代码中使用字符串常量）
 */
object UserFeedback {
    const val HELPFUL = "helpful"           // 有帮助（未执行但认可）
    const val NOT_HELPFUL = "not_helpful"   // 无帮助
    const val EXECUTED = "executed"         // 已执行
    const val DISMISSED = "dismissed"       // 忽略/不相关
}

/**
 * 建议类别枚举
 */
object AdviceCategory {
    const val SLEEP = "sleep"
    const val EXERCISE = "exercise"
    const val DIET = "diet"
    const val EMOTION = "emotion"
    const val SYMPTOM = "symptom"
    const val OTHER = "other"
    
    /**
     * 根据建议文本智能推断类别
     */
    fun inferFromText(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("睡眠") || lowerText.contains("睡觉") || 
            lowerText.contains("入睡") || lowerText.contains("失眠") -> SLEEP
            
            lowerText.contains("运动") || lowerText.contains("步数") || 
            lowerText.contains("散步") || lowerText.contains("活动") -> EXERCISE
            
            lowerText.contains("饮食") || lowerText.contains("吃") || 
            lowerText.contains("饮水") || lowerText.contains("食物") -> DIET
            
            lowerText.contains("情绪") || lowerText.contains("心情") || 
            lowerText.contains("焦虑") || lowerText.contains("放松") -> EMOTION
            
            lowerText.contains("头痛") || lowerText.contains("疼痛") || 
            lowerText.contains("不适") || lowerText.contains("症状") -> SYMPTOM
            
            else -> OTHER
        }
    }
}
