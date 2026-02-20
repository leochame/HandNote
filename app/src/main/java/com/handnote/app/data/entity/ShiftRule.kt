package com.handnote.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_rules")
data class ShiftRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startDate: Long, // 使用 Long 存储时间戳
    val cycleDays: Int,
    val shiftConfig: String, // JSON 字符串存储排班配置
    val skipHoliday: Boolean = false,
    val defaultReminderLevel: Int = 0
)

