package com.handnote.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createTime: Long, // 创建时间戳
    val content: String,
    val imagePaths: String? = null, // JSON 字符串存储图片路径列表，可为空
    val linkedTaskIds: String? = null // JSON 字符串存储关联的任务 ID 列表，可为空
)

