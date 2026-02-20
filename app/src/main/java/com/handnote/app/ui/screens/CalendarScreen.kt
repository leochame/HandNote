package com.handnote.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.ui.components.CalendarView
import com.handnote.app.ui.viewmodel.MainViewModel
import com.handnote.app.util.FileLogger
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    viewModel: MainViewModel
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // 获取任务记录数据
    val taskRecordsByDate by viewModel.taskRecordsByDate.collectAsState()
    
    // 调试日志（安全调用，避免初始化问题）
    LaunchedEffect(Unit) {
        try {
            FileLogger.d("CalendarScreen", "CalendarScreen initialized")
            FileLogger.d("CalendarScreen", "Task records count: ${taskRecordsByDate.size}")
        } catch (e: Exception) {
            // 静默失败，不影响 UI
        }
    }
        
        // 显示该日期的任务
        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        var tasksForDate by remember { mutableStateOf<List<TaskRecord>>(emptyList()) }
        
        // 当选中日期改变时，获取该日期的任务
        LaunchedEffect(selectedDate) {
            try {
                tasksForDate = viewModel.getTaskRecordsByDate(selectedDate)
            } catch (e: Exception) {
                // 如果获取失败，保持空列表
                tasksForDate = emptyList()
            }
        }
    
    // 使用 LazyColumn 替代 Column + verticalScroll，避免嵌套滚动问题
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        item {
            Text(
                text = "日历",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // 日历视图（固定高度，不滚动）
        item {
            CalendarView(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                },
                taskRecordsByDate = taskRecordsByDate,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 选中日期的任务列表
        item {
            Divider()
        }
        
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "选中日期：${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
        
                val level = taskRecordsByDate[dateStr] ?: 0
                val statusText = when (level) {
                    2 -> "有强提醒任务（倒班打卡）"
                    1 -> "有弱提醒任务（纪念日）"
                    else -> "暂无任务"
                }
        
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (tasksForDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            tasksForDate.forEach { task ->
                                Text(
                                    text = "• ${task.sourceType} (Level ${task.reminderLevel})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

