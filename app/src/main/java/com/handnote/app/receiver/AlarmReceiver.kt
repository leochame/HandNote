package com.handnote.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.handnote.app.data.database.AppDatabase
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 闹钟广播接收器
 * 接收 AlarmManager 触发的广播，根据任务等级执行不同的提醒逻辑
 */
class AlarmReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver received broadcast")
        
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val reminderLevel = intent.getIntExtra(EXTRA_REMINDER_LEVEL, 0)
        
        if (taskId == -1L) {
            Log.e(TAG, "Invalid task ID")
            return
        }
        
        scope.launch {
            // 初始化数据库和 Repository
            val database = AppDatabase.getDatabase(context)
            val repository = AppRepository(
                shiftRuleDao = database.shiftRuleDao(),
                anniversaryDao = database.anniversaryDao(),
                taskRecordDao = database.taskRecordDao(),
                postDao = database.postDao(),
                holidayDao = database.holidayDao()
            )
            
            // 获取任务记录
            val taskRecord = repository.getTaskRecordById(taskId)
            
            if (taskRecord == null) {
                Log.e(TAG, "Task record not found: $taskId")
                return@launch
            }
            
            // 检查任务状态
            if (taskRecord.status != "pending") {
                Log.d(TAG, "Task already completed or cancelled: $taskId")
                return@launch
            }
            
            // 根据提醒等级执行不同的逻辑
            when (reminderLevel) {
                2 -> {
                    // Level 2: 强提醒 - 启动前台服务播放闹钟
                    Log.d(TAG, "Triggering Level 2 alarm for task: $taskId")
                    AlarmService.startAlarmService(context, taskRecord)
                }
                1 -> {
                    // Level 1: 弱提醒 - 发送系统通知
                    Log.d(TAG, "Triggering Level 1 notification for task: $taskId")
                    AlarmService.showNotification(context, taskRecord)
                }
                else -> {
                    Log.d(TAG, "Unknown reminder level: $reminderLevel")
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_REMINDER_LEVEL = "reminder_level"
    }
}

