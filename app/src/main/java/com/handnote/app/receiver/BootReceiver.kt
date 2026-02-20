package com.handnote.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.handnote.app.data.database.AppDatabase
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.service.AlarmManagerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机广播接收器
 * 系统重启后，重新注册所有闹钟
 */
class BootReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "System boot completed, restoring alarms")
                restoreAlarms(context)
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "Time changed, restoring alarms")
                restoreAlarms(context)
            }
        }
    }
    
    /**
     * 恢复所有闹钟
     */
    private fun restoreAlarms(context: Context) {
        scope.launch {
            try {
                // 初始化数据库和 Repository
                val database = AppDatabase.getDatabase(context)
                val repository = AppRepository(
                    shiftRuleDao = database.shiftRuleDao(),
                    anniversaryDao = database.anniversaryDao(),
                    taskRecordDao = database.taskRecordDao(),
                    postDao = database.postDao(),
                    holidayDao = database.holidayDao()
                )
                
                // 重新生成任务记录（确保未来几天的任务都已生成）
                val schedulerService = com.handnote.app.service.ShiftSchedulerService(repository)
                schedulerService.generateAllTaskRecords(daysAhead = 30)
                
                // 注册所有待执行的任务到 AlarmManager
                val alarmManagerService = AlarmManagerService(context)
                alarmManagerService.registerAllPendingTasks(repository)
                
                Log.d(TAG, "Alarms restored successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alarms", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}

