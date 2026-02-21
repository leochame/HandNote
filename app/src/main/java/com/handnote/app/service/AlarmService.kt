package com.handnote.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.handnote.app.MainActivity
import com.handnote.app.ui.AlarmActivity
import com.handnote.app.R
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.data.database.AppDatabase
import com.handnote.app.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 闹钟服务
 * 负责管理 AlarmManager 的注册和触发提醒
 */
object AlarmService {
    private const val TAG = "AlarmService"
    internal const val CHANNEL_ID = "handnote_alarm_channel"
    private const val NOTIFICATION_ID_BASE = 1000
    
    /**
     * 初始化通知渠道（Android 8.0+ 需要）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "手账提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒班打卡和纪念日提醒"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 启动强提醒闹钟服务（Level 2）
     */
    fun startAlarmService(context: Context, taskRecord: TaskRecord) {
        val intent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra("task_id", taskRecord.id)
            putExtra("task_title", getTaskTitle(taskRecord))
            putExtra("target_pkg_name", taskRecord.targetPkgName)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    /**
     * 显示弱提醒通知（Level 1）
     */
    fun showNotification(context: Context, taskRecord: TaskRecord) {
        createNotificationChannel(context)
        
        val taskTitle = getTaskTitle(taskRecord)
        
        // 创建点击意图
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskRecord.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("提醒：$taskTitle")
            .setContentText("点击查看详情")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        // 显示通知
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(taskRecord.id.toInt(), notification)
    }
    
    /**
     * 获取任务标题
     */
    private fun getTaskTitle(taskRecord: TaskRecord): String {
        return when (taskRecord.sourceType) {
            "shift_rule" -> "倒班打卡提醒"
            "anniversary" -> "纪念日提醒"
            else -> "任务提醒"
        }
    }
}

/**
 * 前台服务：播放强提醒闹钟
 */
class AlarmForegroundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var taskId: Long = -1
    private var taskTitle: String = ""
    private var targetPkgName: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmForegroundService created")
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        AlarmService.createNotificationChannel(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmForegroundService started")
        
        // 如果是停止动作，则停止闹钟并将任务标记为已完成
        if (intent?.action == ACTION_STOP) {
            markTaskAsCompleted()
            stopAlarm()
            return START_NOT_STICKY
        }

        taskId = intent?.getLongExtra("task_id", -1) ?: -1
        taskTitle = intent?.getStringExtra("task_title") ?: "提醒"
        targetPkgName = intent?.getStringExtra("target_pkg_name")
        
        // 创建前台通知
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 开始播放闹钟
        startAlarm()
        
        return START_NOT_STICKY
    }
    
    /**
     * 创建前台通知
     */
    private fun createForegroundNotification(): Notification {
        // 创建全屏意图（锁屏时强制显示）
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
            putExtra("task_title", taskTitle)
            putExtra("target_pkg_name", targetPkgName)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            taskId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建关闭意图
        val stopIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            taskId.toInt() + 10000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, AlarmService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("提醒：$taskTitle")
            .setContentText("点击关闭闹钟")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "关闭",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
        
        return notification
    }
    
    /**
     * 开始播放闹钟
     */
    private fun startAlarm() {
        try {
            // 播放系统默认闹钟铃声
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer?.apply {
                isLooping = true
                start()
            }
            
            // 震动
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 500, 500, 500, 500),
                    0
                )
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500, 500, 500, 500), 0)
            }
            
            // 启动全屏 Activity
            val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("task_id", taskId)
                putExtra("task_title", taskTitle)
                putExtra("target_pkg_name", targetPkgName)
            }
            startActivity(fullScreenIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm", e)
        }
    }
    
    /**
     * 停止闹钟
     */
    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        vibrator?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }
    
    /**
     * 将当前任务标记为已完成（从 pending -> completed）
     */
    private fun markTaskAsCompleted() {
        if (taskId <= 0) return

        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@AlarmForegroundService)
                val repository = AppRepository(
                    shiftRuleDao = database.shiftRuleDao(),
                    anniversaryDao = database.anniversaryDao(),
                    taskRecordDao = database.taskRecordDao(),
                    postDao = database.postDao(),
                    holidayDao = database.holidayDao()
                )

                val task = repository.getTaskRecordById(taskId)
                if (task != null && task.status == "pending") {
                    repository.updateTaskRecord(task.copy(status = "completed"))
                    Log.d(TAG, "Task $taskId marked as completed")
                } else {
                    Log.d(TAG, "Task $taskId not pending or not found, skip status update")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark task $taskId as completed", e)
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        Log.d(TAG, "AlarmForegroundService destroyed")
    }
    
    companion object {
        private const val TAG = "AlarmForegroundService"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.handnote.app.STOP_ALARM"
    }
}

