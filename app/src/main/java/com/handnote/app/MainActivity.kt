package com.handnote.app

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.handnote.app.data.database.AppDatabase
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.service.AlarmManagerService
import com.handnote.app.service.AlarmService
import com.handnote.app.service.HolidaySyncService
import com.handnote.app.ui.MainScreen
import com.handnote.app.ui.theme.HandNoteTheme
import com.handnote.app.ui.viewmodel.MainViewModel
import com.handnote.app.ui.viewmodel.ViewModelFactory
import com.handnote.app.util.FileLogger
import kotlinx.coroutines.launch
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class MainActivity : ComponentActivity() {

    private var database: AppDatabase? = null
    private var repository: AppRepository? = null

    // 通知权限请求
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "通知权限已授予")
            FileLogger.d(TAG, "通知权限已授予")
        } else {
            Log.w(TAG, "通知权限被拒绝，部分提醒功能可能无法正常工作")
            FileLogger.w(TAG, "通知权限被拒绝，部分提醒功能可能无法正常工作")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "handnote_prefs"
        private const val KEY_LAST_SYNC_YEAR = "last_sync_year"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化文件日志系统（必须在最前面）
        try {
            FileLogger.init(applicationContext)
            Log.d(TAG, "FileLogger initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FileLogger", e)
            e.printStackTrace()
        }
        
        // 保存默认的异常处理器
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        // 设置全局未捕获异常处理器（在 FileLogger 初始化之后）
        val appContext = applicationContext
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val errorMsg = "=== CRASH REPORT ===\n" +
                        "Thread: ${thread.name}\n" +
                        "Exception: ${exception.javaClass.name}\n" +
                        "Message: ${exception.message}\n" +
                        "Stack trace:\n${exception.stackTraceToString()}\n" +
                        "==================="
                
                // 输出完整崩溃信息到 Logcat（方便调试）
                Log.e(TAG, errorMsg)
                
                // 写入日志文件（本地存储）
                FileLogger.e(TAG, "CRASH: Uncaught exception in thread ${thread.name}", exception)
                
                // 额外写入崩溃报告文件
                writeCrashReport(appContext, exception)
                
                // 自动导出日志到 Downloads（方便查看）
                try {
                    FileLogger.exportToDownloads(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to auto-export logs", e)
                }
                
                // 输出日志文件路径到 Logcat，方便调试时查看
                try {
                    val latestLog = FileLogger.getLatestLogFile(appContext)
                    val crashReport = File(appContext.filesDir, "crash_report.txt")
                    Log.e(TAG, "=== LOG FILES ===")
                    Log.e(TAG, "Log file: ${latestLog?.absolutePath}")
                    Log.e(TAG, "Crash report: ${crashReport.absolutePath}")
                    Log.e(TAG, "================")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get log file path", e)
                }
            } catch (e: Exception) {
                // 如果日志记录失败，至少输出到 Logcat
                Log.e(TAG, "Failed to log exception", e)
                e.printStackTrace()
            }
            
            // 调用默认处理器，让应用正常崩溃，避免进入异常状态
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Log.d(TAG, "MainActivity.onCreate started")

        try {
            Log.d(TAG, "Starting database initialization...")
            FileLogger.d(TAG, "Starting database initialization...")
            
            // 初始化数据库（使用单例模式）
            try {
                database = AppDatabase.getDatabase(applicationContext)
                Log.d(TAG, "Database initialized successfully")
                FileLogger.d(TAG, "Database initialized successfully")
            } catch (dbError: Exception) {
                Log.e(TAG, "Database initialization failed", dbError)
                FileLogger.e(TAG, "Database initialization failed", dbError)
                throw IllegalStateException("Database initialization failed: ${dbError.message}", dbError)
            }

            // 初始化 Repository
            Log.d(TAG, "Starting repository initialization...")
            FileLogger.d(TAG, "Starting repository initialization...")
            val db = database
            if (db != null) {
                try {
                    repository = AppRepository(
                        shiftRuleDao = db.shiftRuleDao(),
                        anniversaryDao = db.anniversaryDao(),
                        taskRecordDao = db.taskRecordDao(),
                        postDao = db.postDao(),
                        holidayDao = db.holidayDao()
                    )
                    Log.d(TAG, "Repository initialized successfully")
                    FileLogger.d(TAG, "Repository initialized successfully")
                } catch (repoError: Exception) {
                    Log.e(TAG, "Repository initialization failed", repoError)
                    FileLogger.e(TAG, "Repository initialization failed", repoError)
                    throw IllegalStateException("Repository initialization failed: ${repoError.message}", repoError)
                }
            } else {
                throw IllegalStateException("Database instance is null")
            }
            
            // 初始化通知渠道
            try {
                AlarmService.createNotificationChannel(this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
                FileLogger.e(TAG, "Failed to create notification channel", e)
            }

            // 请求通知权限（Android 13+）
            requestNotificationPermission()
            
            // 注册所有待执行的任务到 AlarmManager
            val repo = repository
            if (repo != null) {
                lifecycleScope.launch {
                    try {
                        val alarmManagerService = AlarmManagerService(this@MainActivity)
                        alarmManagerService.registerAllPendingTasks(repo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to register pending tasks", e)
                        FileLogger.e(TAG, "Failed to register pending tasks", e)
                    }
                }
                
                // 自动同步节假日数据（后台静默执行，不阻塞UI）
                lifecycleScope.launch {
                    try {
                        checkAndSyncHolidays(repo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync holidays automatically", e)
                        FileLogger.e(TAG, "Failed to sync holidays automatically", e)
                    }
                }
            }

            // 确保 repository 已初始化后再设置 UI
            Log.d(TAG, "Setting up UI...")
            val finalRepository = repository
            if (finalRepository != null) {
                val viewModelFactory = ViewModelFactory(finalRepository, this@MainActivity)
                Log.d(TAG, "ViewModelFactory created, calling setContent...")
                setContent {
                    Log.d(TAG, "setContent lambda called")
                    HandNoteTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainScreen(viewModelFactory = viewModelFactory)
                        }
                    }
                }
                Log.d(TAG, "setContent completed")
            } else {
                throw IllegalStateException("Repository initialization failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize app", e)
            FileLogger.e(TAG, "Failed to initialize app", e)
            e.printStackTrace()
            // 写入崩溃报告
            try {
                writeCrashReport(applicationContext, e)
            } catch (reportError: Exception) {
                Log.e(TAG, "Failed to write crash report", reportError)
            }
            // 即使初始化失败，也显示 UI，但可能功能受限
            try {
                setContent {
                    HandNoteTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            ErrorScreen(
                                exception = e,
                                onViewLogs = {
                                    try {
                                        val intent = android.content.Intent(this@MainActivity, com.handnote.app.ui.LogViewerActivity::class.java)
                                        startActivity(intent)
                                    } catch (intentError: Exception) {
                                        Log.e(TAG, "Failed to open LogViewerActivity", intentError)
                                    }
                                }
                            )
                        }
                    }
                }
            } catch (uiError: Exception) {
                Log.e(TAG, "Failed to show error UI", uiError)
                // 如果连错误 UI 都显示不了，至少记录日志
            }
        }
    }
    
    /**
     * 写入崩溃报告到文件
     * 同时保存到应用内部存储和 Downloads 文件夹（方便查看）
     */
    private fun writeCrashReport(context: android.content.Context, exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date())
            
            val report = """
                ========================================
                CRASH REPORT
                ========================================
                Timestamp: $timestamp
                Exception: ${exception.javaClass.name}
                Message: ${exception.message}
                
                Stack Trace:
                ${exception.stackTraceToString()}
                
                ========================================
            """.trimIndent()
            
            // 1. 保存到应用内部存储（原有逻辑）
            try {
                val crashFile = File(context.filesDir, "crash_report.txt")
            crashFile.writeText(report)
            Log.d(TAG, "Crash report written to: ${crashFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash report to internal storage", e)
            }
            
            // 2. 同时保存到 Downloads 文件夹（方便直接查看）
            // 使用应用特定的 Downloads 目录（Android 10+ 推荐方式，无需权限）
            try {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    
                    val timestampForFile = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                        .format(Date())
                    val crashExportFile = File(downloadsDir, "HandNote_crash_$timestampForFile.txt")
                    crashExportFile.writeText(report)
                    Log.d(TAG, "Crash report written to Downloads: ${crashExportFile.absolutePath}")
                } else {
                    Log.w(TAG, "Failed to get Downloads directory, skipping Downloads export")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash report to Downloads", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash report", e)
        }
    }
    
    /**
     * 检查并自动同步节假日数据
     * 策略：
     * 1. 首次启动：同步当前年份及前后各一年
     * 2. 跨年：如果当前年份未同步过，自动同步
     * 3. 每月检查一次（可选，避免频繁同步）
     */
    private suspend fun checkAndSyncHolidays(repository: AppRepository) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val lastSyncYear = prefs.getInt(KEY_LAST_SYNC_YEAR, 0)
            val currentYear = LocalDate.now().year
            
            // 如果从未同步过，或者当前年份未同步过，则进行同步
            if (lastSyncYear == 0 || lastSyncYear < currentYear) {
                Log.d(TAG, "检测到需要同步节假日数据（上次同步年份: $lastSyncYear, 当前年份: $currentYear）")
                FileLogger.d(TAG, "检测到需要同步节假日数据（上次同步年份: $lastSyncYear, 当前年份: $currentYear）")
                
                val syncService = HolidaySyncService(repository)
                val result = syncService.syncCurrentYearRange()
                
                if (result.success) {
                    // 更新最后同步年份
                    prefs.edit().putInt(KEY_LAST_SYNC_YEAR, currentYear).apply()
                    Log.d(TAG, "节假日数据自动同步成功: ${result.message}")
                    FileLogger.d(TAG, "节假日数据自动同步成功: ${result.message}")
                } else {
                    Log.w(TAG, "节假日数据自动同步失败: ${result.message}")
                    FileLogger.w(TAG, "节假日数据自动同步失败: ${result.message}")
                }
            } else {
                Log.d(TAG, "节假日数据已是最新（上次同步年份: $lastSyncYear）")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查节假日同步状态时出错", e)
            FileLogger.e(TAG, "检查节假日同步状态时出错", e)
        }
    }

    /**
     * 请求通知权限（Android 13+ 需要动态请求）
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "通知权限已授予")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 用户之前拒绝过，可以显示解释说明
                    Log.d(TAG, "需要向用户解释为什么需要通知权限")
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 首次请求权限
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

/**
 * 错误显示界面
 */
@Composable
private fun ErrorScreen(
    exception: Throwable,
    onViewLogs: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "应用初始化失败",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "错误类型:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = exception.javaClass.simpleName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "错误消息:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = exception.message ?: "无错误消息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "堆栈跟踪:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = exception.stackTraceToString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                )
            }
        }
        
        Button(
            onClick = onViewLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("查看详细日志")
        }
        
        Text(
            text = "提示: 日志文件已保存，可以通过设置页面查看",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

