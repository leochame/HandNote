package com.handnote.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.handnote.app.ui.AlarmActivity
import com.handnote.app.receiver.AlarmReceiver
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.data.repository.AppRepository
import kotlinx.coroutines.flow.first

/**
 * AlarmManager 管理服务
 * 负责将 TaskRecord 注册到系统的 AlarmManager
 */
class AlarmManagerService(private val context: Context) {
    
    private val alarmManager: AlarmManager = (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
        ?: throw IllegalStateException("AlarmManager service not available")
    
    /**
     * 注册单个任务到 AlarmManager
     */
    fun registerTask(taskRecord: TaskRecord) {
        val currentTime = System.currentTimeMillis()
        
        // 只注册未来的任务
        if (taskRecord.triggerTimestamp <= currentTime) {
            Log.d(TAG, "Task ${taskRecord.id} trigger time has passed, skipping")
            return
        }
        
        // 只注册待执行的任务
        if (taskRecord.status != "pending") {
            Log.d(TAG, "Task ${taskRecord.id} is not pending, skipping")
            return
        }
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskRecord.id)
            putExtra(AlarmReceiver.EXTRA_REMINDER_LEVEL, taskRecord.reminderLevel)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskRecord.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            when (taskRecord.reminderLevel) {
                2 -> {
                    // Level 2: 强提醒 - 使用 setAlarmClock（最高优先级）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(
                            taskRecord.triggerTimestamp,
                            createAlarmInfoPendingIntent(taskRecord)
                        )
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, taskRecord.triggerTimestamp, pendingIntent)
                    }
                    Log.d(TAG, "Registered Level 2 alarm for task ${taskRecord.id} at ${taskRecord.triggerTimestamp}")
                }
                1 -> {
                    // Level 1: 弱提醒 - 使用 setExactAndAllowWhileIdle
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            taskRecord.triggerTimestamp,
                            pendingIntent
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, taskRecord.triggerTimestamp, pendingIntent)
                    }
                    Log.d(TAG, "Registered Level 1 alarm for task ${taskRecord.id} at ${taskRecord.triggerTimestamp}")
                }
                else -> {
                    Log.d(TAG, "Task ${taskRecord.id} has reminder level ${taskRecord.reminderLevel}, skipping")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register alarm for task ${taskRecord.id}", e)
        }
    }
    
    /**
     * 创建 AlarmClockInfo 的 PendingIntent（用于显示在锁屏上）
     */
    private fun createAlarmInfoPendingIntent(taskRecord: TaskRecord): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskRecord.id)
            putExtra("task_title", "倒班打卡提醒")
            putExtra("target_pkg_name", taskRecord.targetPkgName)
        }
        
        return PendingIntent.getActivity(
            context,
            taskRecord.id.toInt() + 50000, // 使用不同的 request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * 取消任务注册
     */
    fun cancelTask(taskRecord: TaskRecord) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskRecord.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for task ${taskRecord.id}")
    }
    
    /**
     * 注册所有待执行的任务
     */
    suspend fun registerAllPendingTasks(repository: AppRepository) {
        try {
            val allTasks = try {
                repository.getAllTaskRecords().first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get task records", e)
                emptyList()
            }
            
            val currentTime = System.currentTimeMillis()
            
            val pendingTasks = allTasks.filter { task ->
                task.status == "pending" && task.triggerTimestamp > currentTime
            }
            
            Log.d(TAG, "Registering ${pendingTasks.size} pending tasks")
            
            pendingTasks.forEach { task ->
                try {
                    registerTask(task)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register task ${task.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register all pending tasks", e)
        }
    }
    
    companion object {
        private const val TAG = "AlarmManagerService"
    }
}

