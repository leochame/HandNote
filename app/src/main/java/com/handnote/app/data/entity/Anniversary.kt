package com.handnote.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anniversaries")
data class Anniversary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetDate: String, // 使用 String 存储日期，格式如 "2024-01-01"
    val reminderLevel: Int = 0,
    val reminderTime: Long? = null // 提醒时间戳，可为空
)

