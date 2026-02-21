package com.handnote.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.handnote.app.ui.LogViewerActivity
import com.handnote.app.ui.viewmodel.HolidaySyncState
import com.handnote.app.ui.viewmodel.MainViewModel
import com.handnote.app.util.FileLogger
import com.handnote.app.util.GmailPrefs
import java.io.File

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val syncState by viewModel.holidaySyncState.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Divider()

        // Gmail + AI 集成配置
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            var apiKey by remember { mutableStateOf(GmailPrefs.getGeminiApiKey(context) ?: "") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Gmail 邮件助手",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "配置 Gemini API Key 以使用 AI 总结邮件和识别面试安排。可从 https://ai.google.dev 免费获取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Button(
                    onClick = {
                        GmailPrefs.setGeminiApiKey(context, apiKey.ifBlank { null })
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存 API Key")
                }
            }
        }
        
        // 节假日同步卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "节假日同步",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "同步中国法定节假日数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 同步按钮
                    val currentSyncState = syncState
                    Button(
                        onClick = { viewModel.syncHolidays() },
                        enabled = currentSyncState !is HolidaySyncState.Syncing
                    ) {
                        when (currentSyncState) {
                            is HolidaySyncState.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            else -> {
                                Text("同步")
                            }
                        }
                    }
                }
                
                // 同步状态显示
                val currentSyncStateForDisplay = syncState
                when (currentSyncStateForDisplay) {
                    is HolidaySyncState.Success -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ ${currentSyncStateForDisplay.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetHolidaySyncState() }
                                ) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                    is HolidaySyncState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✗ ${currentSyncStateForDisplay.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetHolidaySyncState() }
                                ) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        
        // 日志查看器按钮
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val intent = Intent(context, LogViewerActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "查看日志",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "查看应用日志和崩溃报告",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 导出日志到 Downloads 按钮
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val exportPath = FileLogger.exportToDownloads(context)
                if (exportPath != null) {
                    Toast.makeText(
                        context,
                        "日志已导出到 Downloads 文件夹\n$exportPath",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "导出失败：没有找到日志文件",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "导出日志到 Downloads",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "将日志和崩溃报告导出到 Downloads 文件夹，方便查看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 显示日志文件路径（本地存储）
        val latestLogFile = remember(context) { com.handnote.app.util.FileLogger.getLatestLogFile(context) }
        val logPath = latestLogFile?.absolutePath ?: "未找到日志文件"
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "日志文件路径",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = logPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "日志直接保存在应用本地目录，可通过 adb 或文件管理器查看",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

