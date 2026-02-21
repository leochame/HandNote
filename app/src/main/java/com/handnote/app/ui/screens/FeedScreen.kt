package com.handnote.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.ui.viewmodel.FeedItem
import com.handnote.app.ui.viewmodel.FeedItemType
import com.handnote.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    viewModel: MainViewModel
) {
    val feedItems by viewModel.feedItems.collectAsState()

    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        // 使用 Box 明确提供尺寸约束，并应用 padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (feedItems.isEmpty()) {
                // 空状态：居中显示提示文本
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "还没有任何记录，可以先写一篇手账。")
                }
            } else {
                // 有数据：显示 LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(feedItems, key = { it.id }) { item ->
                        when (item.type) {
                            FeedItemType.POST -> {
                                item.post?.let { post ->
                                    PostCard(
                                        content = post.content,
                                        createTime = post.createTime
                                    )
                                }
                            }
                            FeedItemType.TASK -> {
                                item.task?.let { task ->
                                    TaskRecordCard(taskRecord = task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        PostEditorDialog(
            onDismiss = { showEditor = false },
            onSave = { content ->
                viewModel.addPost(content)
                showEditor = false
            }
        )
    }
}

@Composable
private fun PostCard(
    content: String,
    createTime: Long
) {
    val formatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }
    val timeText = formatter.format(Date(createTime))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun TaskRecordCard(
    taskRecord: TaskRecord
) {
    val formatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }
    val timeText = formatter.format(Date(taskRecord.triggerTimestamp))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "任务完成",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = taskRecord.targetDate,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "来源：${taskRecord.sourceType}  •  提醒等级：${taskRecord.reminderLevel}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "触发时间：$timeText",
                style = MaterialTheme.typography.bodySmall
            )
            if (taskRecord.targetPkgName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "关联应用：${taskRecord.targetPkgName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PostEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "新建手账",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "后续可以在这里扩展图片、关联任务等字段。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = { onSave(content) },
                        enabled = content.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

