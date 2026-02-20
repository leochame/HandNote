package com.handnote.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey
    val date: String, // 日期，格式如 "2024-01-01"，作为主键
    val type: String, // 节假日类型，如 "national", "regional" 等
    val name: String // 节假日名称
)

