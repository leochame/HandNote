package com.handnote.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.handnote.app.util.FileLogger
import java.io.File

/**
 * 日志查看器 Activity
 * 用于查看应用日志和崩溃报告
 */
class LogViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LogViewerScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen() {
    val context = LocalContext.current
    var logContent by remember { mutableStateOf<String>("加载中...") }
    var crashReportContent by remember { mutableStateOf<String>("") }
    var selectedTab by remember { mutableStateOf(0) }
    var exportedLogFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var exportedCrashFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedExportedFile by remember { mutableStateOf<File?>(null) }
    var exportedFileContent by remember { mutableStateOf<String>("") }
    
    LaunchedEffect(Unit) {
        try {
            // 加载日志文件
            val latestLog = FileLogger.getLatestLogFile(context)
            logContent = if (latestLog != null && latestLog.exists()) {
                latestLog.readText()
            } else {
                "没有找到日志文件"
            }
            
            // 加载崩溃报告
            val crashFile = File(context.filesDir, "crash_report.txt")
            crashReportContent = if (crashFile.exists()) {
                crashFile.readText()
            } else {
                "没有崩溃报告"
            }
            
            // 加载已导出的日志文件列表
            exportedLogFiles = FileLogger.getExportedLogFiles(context)
            exportedCrashFiles = FileLogger.getExportedCrashFiles(context)
        } catch (e: Exception) {
            logContent = "加载日志失败: ${e.message}\n${e.stackTraceToString()}"
        }
    }
    
    // 当选择已导出文件时，加载文件内容
    LaunchedEffect(selectedExportedFile) {
        selectedExportedFile?.let { file ->
            exportedFileContent = FileLogger.readExportedLogFile(file) ?: "无法读取文件内容"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志查看器") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 标签页
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("应用日志") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("崩溃报告") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("已导出日志") }
                )
            }
            
            // 内容区域
            when (selectedTab) {
                0 -> {
                    LogContent(logContent)
                }
                1 -> {
                    CrashReportContent(crashReportContent)
                }
                2 -> {
                    ExportedLogsContent(
                        exportedLogFiles = exportedLogFiles,
                        exportedCrashFiles = exportedCrashFiles,
                        selectedFile = selectedExportedFile,
                        fileContent = exportedFileContent,
                        onFileSelected = { file ->
                            selectedExportedFile = file
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LogContent(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = content,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CrashReportContent(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (content.contains("没有崩溃报告")) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = content,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedLogsContent(
    exportedLogFiles: List<File>,
    exportedCrashFiles: List<File>,
    selectedFile: File?,
    fileContent: String,
    onFileSelected: (File) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // 左侧：文件列表
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Text(
                text = "已导出日志文件",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (exportedLogFiles.isEmpty() && exportedCrashFiles.isEmpty()) {
                Text(
                    text = "没有已导出的日志文件\n\n请在设置中导出日志",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (exportedLogFiles.isNotEmpty()) {
                        Text(
                            text = "日志文件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        exportedLogFiles.forEach { file ->
                            Card(
                                onClick = { onFileSelected(file) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedFile == file) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "${file.length() / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (exportedCrashFiles.isNotEmpty()) {
                        Text(
                            text = "崩溃报告",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        exportedCrashFiles.forEach { file ->
                            Card(
                                onClick = { onFileSelected(file) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedFile == file) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "${file.length() / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        // 右侧：文件内容
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            if (selectedFile != null) {
                Text(
                    text = selectedFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = fileContent,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "请从左侧选择一个文件查看",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

