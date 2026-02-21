package com.handnote.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件日志工具类
 * 将日志同时输出到 Logcat 和文件
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val LOG_DIR_NAME = "logs"
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_LOG_FILES = 10 // 最多保留10个日志文件
    private const val MAX_CRASH_FILES = 5 // 最多保留5个崩溃报告
    
    @Volatile
    private var initialized = false
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 初始化日志系统
     */
    fun init(context: Context) {
        try {
            val dir = File(context.filesDir, LOG_DIR_NAME)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created && !dir.exists()) {
                    Log.e(TAG, "Failed to create log directory: ${dir.absolutePath}")
                    return
                }
            }
            
            // 确保目录创建成功后再设置 logDir
            logDir = dir
            
            // 清理旧日志文件（此时 logDir 已设置）
            cleanupOldLogs()

            // 清理旧崩溃报告
            cleanupOldCrashReports(context)
            
            // 创建或获取当前日志文件
            val today = fileDateFormat.format(Date())
            val logFile = File(logDir, "app_$today.log")
            
            // 如果文件太大，创建新的日志文件
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                val timestamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                currentLogFile = File(logDir, "app_${today}_$timestamp.log")
            } else {
                currentLogFile = logFile
            }
            
            // 标记为已初始化
            initialized = true
            
            // 使用 Log.d 而不是 FileLogger.d，避免在初始化过程中循环调用
            Log.d(TAG, "FileLogger initialized. Log file: ${currentLogFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FileLogger", e)
            // 确保即使初始化失败，也不会导致后续调用崩溃
            initialized = false
            logDir = null
            currentLogFile = null
        }
    }
    
    /**
     * 清理旧的日志文件
     */
    private fun cleanupOldLogs() {
        try {
            val logDir = this.logDir ?: return
            val logFiles = logDir.listFiles { file ->
                file.isFile && file.name.startsWith("app_") && file.name.endsWith(".log")
            } ?: return

            // 按修改时间排序，最新的在前
            logFiles.sortByDescending { it.lastModified() }

            // 删除超过最大数量的旧文件
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old log file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }

    /**
     * 清理旧的崩溃报告文件
     */
    private fun cleanupOldCrashReports(context: Context) {
        try {
            // 清理应用内部存储中的崩溃报告
            val internalDir = context.filesDir
            cleanupCrashFilesInDir(internalDir)

            // 清理 Downloads 目录中的崩溃报告
            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                cleanupCrashFilesInDir(downloadsDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old crash reports", e)
        }
    }

    private fun cleanupCrashFilesInDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        try {
            val crashFiles = dir.listFiles { file ->
                file.isFile && (file.name.startsWith("crash_report") || file.name.contains("crash"))
            } ?: return

            // 按修改时间排序，最新的在前
            crashFiles.sortByDescending { it.lastModified() }

            // 删除超过最大数量的旧文件
            if (crashFiles.size > MAX_CRASH_FILES) {
                crashFiles.drop(MAX_CRASH_FILES).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old crash report: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup crash files in ${dir.absolutePath}", e)
        }
    }
    
    /**
     * 写入日志到文件（同时输出到 Logcat）
     */
    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable? = null) {
        // 如果日志系统未初始化，只输出到 Logcat，不写入文件
        if (!initialized) return
        
        try {
            val logFile = currentLogFile ?: return
            val timestamp = dateFormat.format(Date())
            val logEntry = buildString {
                append("[$timestamp] ")
                append("[$level] ")
                append("[$tag] ")
                append(message)
                if (throwable != null) {
                    append("\n")
                    append(throwable.stackTraceToString())
                }
                append("\n")
            }
            
            // 写入文件
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry)
                writer.flush()
            }
        } catch (e: IOException) {
            // 文件写入失败时，只输出到 Logcat，避免循环错误
            Log.e(TAG, "Failed to write log to file", e)
        } catch (e: Exception) {
            // 捕获所有异常，避免日志系统本身导致崩溃
            Log.e(TAG, "Unexpected error in writeToFile", e)
        }
    }
    
    /**
     * Debug 级别日志
     */
    fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
            if (initialized) {
                writeToFile("DEBUG", tag, message)
            }
        } catch (e: Exception) {
            // 静默失败，避免日志系统本身导致崩溃
        }
    }
    
    /**
     * Info 级别日志
     */
    fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
            if (initialized) {
                writeToFile("INFO", tag, message)
            }
        } catch (e: Exception) {
            // 静默失败，避免日志系统本身导致崩溃
        }
    }
    
    /**
     * Warning 级别日志
     */
    fun w(tag: String, message: String) {
        try {
            Log.w(tag, message)
            if (initialized) {
                writeToFile("WARN", tag, message)
            }
        } catch (e: Exception) {
            // 静默失败，避免日志系统本身导致崩溃
        }
    }
    
    /**
     * Error 级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
            if (initialized) {
                writeToFile("ERROR", tag, message, throwable)
            }
        } catch (e: Exception) {
            // 静默失败，避免日志系统本身导致崩溃
        }
    }
    
    /**
     * 获取日志文件路径列表
     */
    fun getLogFiles(context: Context): List<File> {
        return try {
        val logDir = File(context.filesDir, LOG_DIR_NAME)
        if (!logDir.exists()) {
            return emptyList()
        }
        
            logDir.listFiles { file ->
            file.isFile && file.name.startsWith("app_") && file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get log files", e)
            emptyList()
        }
    }
    
    /**
     * 获取最新的日志文件路径
     */
    fun getLatestLogFile(context: Context): File? {
        return try {
            getLogFiles(context).firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest log file", e)
            null
        }
    }
    
    /**
     * 导出日志到外部存储（需要权限）
     */
    fun exportLogToExternalStorage(context: Context, targetFile: File): Boolean {
        return try {
            val latestLog = getLatestLogFile(context) ?: return false
            latestLog.copyTo(targetFile, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export log", e)
            false
        }
    }
    
    /**
     * 自动导出日志和崩溃报告到 Downloads 文件夹
     * 将所有日志文件合并到一个文件中，方便查看
     * 这样用户可以通过文件管理器直接访问
     * 
     * 兼容性说明：
     * - Android 10+ (API 29+): 使用应用特定的 Downloads 目录，无需权限
     * - 文件保存在: /storage/emulated/0/Android/data/com.handnote.app/files/Download/
     * - 用户可以通过文件管理器访问，应用卸载时文件会被删除
     */
    fun exportToDownloads(context: Context): String? {
        return try {
            // 使用应用特定的 Downloads 目录（Android 10+ 推荐方式，无需权限）
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return null.also { Log.e(TAG, "Failed to get Downloads directory") }
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            
            // 获取所有日志文件（按时间排序，从旧到新）
            val allLogFiles = getLogFiles(context).reversed()
            
            // 合并所有日志文件到一个文件
            val logExportFile = File(downloadsDir, "HandNote_log_$timestamp.txt")
            if (allLogFiles.isNotEmpty()) {
                FileWriter(logExportFile).use { writer ->
                    writer.append("========================================\n")
                    writer.append("HandNote 日志导出\n")
                    writer.append("导出时间: $timestamp\n")
                    writer.append("日志文件数量: ${allLogFiles.size}\n")
                    writer.append("========================================\n\n")
                    
                    allLogFiles.forEachIndexed { index, logFile ->
                        writer.append("\n")
                        writer.append("========================================\n")
                        writer.append("日志文件 ${index + 1}/${allLogFiles.size}: ${logFile.name}\n")
                        writer.append("========================================\n")
                        writer.append("\n")
                        
                        try {
                            logFile.readText().let { content ->
                                writer.append(content)
                                if (!content.endsWith("\n")) {
                                    writer.append("\n")
                                }
                            }
                        } catch (e: Exception) {
                            writer.append("读取日志文件失败: ${e.message}\n")
                            Log.e(TAG, "Failed to read log file: ${logFile.name}", e)
                        }
                        
                        writer.append("\n")
                    }
                    
                    writer.flush()
                }
            } else {
                // 即使没有日志文件，也创建一个空文件说明情况
                FileWriter(logExportFile).use { writer ->
                    writer.append("========================================\n")
                    writer.append("HandNote 日志导出\n")
                    writer.append("导出时间: $timestamp\n")
                    writer.append("日志文件数量: 0\n")
                    writer.append("========================================\n\n")
                    writer.append("当前没有日志文件。\n")
                    writer.flush()
                }
            }
            
            // 导出崩溃报告
            val crashFile = File(context.filesDir, "crash_report.txt")
            val crashExportFile = File(downloadsDir, "HandNote_crash_$timestamp.txt")
            if (crashFile.exists()) {
                crashFile.copyTo(crashExportFile, overwrite = true)
            }
            
            // 返回导出路径
            if (logExportFile.exists()) {
                logExportFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export to Downloads", e)
            null
        }
    }
    
    /**
     * 获取 Downloads 目录中已导出的日志文件列表
     */
    fun getExportedLogFiles(context: Context): List<File> {
        return try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return emptyList()
            
            if (!downloadsDir.exists()) {
                return emptyList()
            }
            
            downloadsDir.listFiles { file ->
                file.isFile && file.name.startsWith("HandNote_log_") && file.name.endsWith(".txt")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get exported log files", e)
            emptyList()
        }
    }
    
    /**
     * 获取 Downloads 目录中已导出的崩溃报告文件列表
     */
    fun getExportedCrashFiles(context: Context): List<File> {
        return try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return emptyList()
            
            if (!downloadsDir.exists()) {
                return emptyList()
            }
            
            downloadsDir.listFiles { file ->
                file.isFile && file.name.startsWith("HandNote_crash_") && file.name.endsWith(".txt")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get exported crash files", e)
            emptyList()
        }
    }
    
    /**
     * 读取已导出的日志文件内容
     */
    fun readExportedLogFile(file: File): String? {
        return try {
            if (file.exists() && file.canRead()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read exported log file: ${file.name}", e)
            null
        }
    }
    
    /**
     * 清除所有日志文件
     */
    fun clearAllLogs(context: Context) {
        try {
            val logDir = File(context.filesDir, LOG_DIR_NAME)
            if (logDir.exists()) {
                logDir.listFiles()?.forEach { it.delete() }
            }
            d(TAG, "All logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}

