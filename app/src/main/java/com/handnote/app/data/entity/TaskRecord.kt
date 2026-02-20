package com.handnote.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_records")
data class TaskRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceType: String, // 来源类型，如 "shift_rule", "anniversary" 等
    val sourceId: Long, // 来源 ID
    val targetDate: String, // 目标日期，格式如 "2024-01-01"
    val triggerTimestamp: Long, // 触发时间戳
    val reminderLevel: Int = 0,
    val status: String, // 状态，如 "pending", "completed", "cancelled"
    val targetPkgName: String? = null // 目标包名，可为空
)

