package com.handnote.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.handnote.app.data.entity.Anniversary
import com.handnote.app.data.entity.Post
import com.handnote.app.data.entity.ShiftRule
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.service.AlarmManagerService
import com.handnote.app.service.HolidaySyncService
import com.handnote.app.service.ShiftSchedulerService
import com.handnote.app.util.FileLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainViewModel(
    private val repository: AppRepository,
    private val context: Context
) : ViewModel() {

    private val schedulerService = ShiftSchedulerService(repository)
    private val holidaySyncService = HolidaySyncService(repository)
    private val alarmManagerService = AlarmManagerService(context)

    // 所有排班规则（安全初始化，捕获异常）
    val allShiftRules: StateFlow<List<ShiftRule>> = try {
        repository.getAllShiftRules()
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error loading shift rules", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize allShiftRules", e)
        MutableStateFlow<List<ShiftRule>>(emptyList()).asStateFlow()
    }

    // 所有纪念日（安全初始化，捕获异常）
    val allAnniversaries: StateFlow<List<Anniversary>> = try {
        repository.getAllAnniversaries()
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error loading anniversaries", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize allAnniversaries", e)
        MutableStateFlow(emptyList())
    }
    
    // 所有任务记录的 Flow（安全初始化，捕获异常）
    val allTaskRecords: StateFlow<List<TaskRecord>> = try {
        repository.getAllTaskRecords()
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error loading task records", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize allTaskRecords", e)
        MutableStateFlow(emptyList())
    }

    // 所有手账 Post（安全初始化，捕获异常）
    val allPosts: StateFlow<List<Post>> = try {
        repository.getAllPosts()
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error loading posts", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize allPosts", e)
        MutableStateFlow(emptyList())
    }

    // Feed 信息流：将 Post 与已完成的 TaskRecord 合并为时间线（安全初始化）
    val feedItems: StateFlow<List<FeedItem>> = try {
        combine(allPosts, allTaskRecords) { posts, tasks ->
            try {
                val postItems = posts.map { post ->
                    FeedItem(
                        id = "post-${post.id}",
                        type = FeedItemType.POST,
                        timestamp = post.createTime,
                        post = post,
                        task = null
                    )
                }
                val taskItems = tasks
                    .filter { it.status == "completed" }
                    .map { task ->
                        FeedItem(
                            id = "task-${task.id}",
                            type = FeedItemType.TASK,
                            timestamp = task.triggerTimestamp,
                            post = null,
                            task = task
                        )
                    }
                (postItems + taskItems).sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error combining feed items", e)
                emptyList()
            }
        }
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error in feedItems flow", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize feedItems", e)
        MutableStateFlow<List<FeedItem>>(emptyList()).asStateFlow()
    }
    
    // 按日期分组的任务记录（日期 -> 最高提醒等级）（安全初始化）
    val taskRecordsByDate: StateFlow<Map<String, Int>> = try {
        allTaskRecords
            .map { records ->
                try {
                    records.groupBy { it.targetDate }
                        .mapValues { (_, records) ->
                            // 获取该日期最高的提醒等级
                            records.maxOfOrNull { it.reminderLevel } ?: 0
                        }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error mapping task records by date", e)
                    emptyMap()
                }
            }
            .catch { e ->
                android.util.Log.e("MainViewModel", "Error in taskRecordsByDate flow", e)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Failed to initialize taskRecordsByDate", e)
        MutableStateFlow<Map<String, Int>>(emptyMap()).asStateFlow()
    }
    
    init {
        // ViewModel 初始化时，根据当前配置生成未来一段时间的任务记录
        // 延迟执行，避免阻塞 UI 启动
        viewModelScope.launch {
            try {
                // 延迟更长时间，确保数据库和 Repository 完全初始化
                kotlinx.coroutines.delay(500)
                generateFutureTaskRecords()
            } catch (e: Exception) {
                // 记录错误但不崩溃，允许应用继续运行
                try {
                    FileLogger.e("MainViewModel", "Failed to generate future task records", e)
                } catch (logError: Exception) {
                    // 如果日志记录也失败，至少输出到 Logcat
                    android.util.Log.e("MainViewModel", "Failed to generate future task records", e)
                }
            }
        }
    }
    
    /**
     * 获取指定日期的任务记录
     */
    suspend fun getTaskRecordsByDate(date: LocalDate): List<TaskRecord> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return repository.getTaskRecordsByDate(dateStr)
    }
    
    /**
     * 根据当前排班规则生成未来一段时间的任务记录
     */
    private suspend fun generateFutureTaskRecords(daysAhead: Int = 30) {
        // 仅基于当前用户配置的排班规则生成未来任务，不再自动创建测试数据
        schedulerService.generateAllTaskRecords(daysAhead = daysAhead)
        // 注册所有待执行任务到系统闹钟
        alarmManagerService.registerAllPendingTasks(repository)
    }
    
    // ---------------- ShiftRule CRUD ----------------

    fun upsertShiftRule(shiftRule: ShiftRule) {
        viewModelScope.launch {
            if (shiftRule.id == 0L) {
                repository.insertShiftRule(shiftRule)
            } else {
                repository.updateShiftRule(shiftRule)
            }
            // 规则变更后，重新生成未来任务
            generateFutureTaskRecords(daysAhead = 30)
        }
    }

    fun deleteShiftRule(shiftRule: ShiftRule) {
        viewModelScope.launch {
            repository.deleteShiftRule(shiftRule)
            generateFutureTaskRecords(daysAhead = 30)
        }
    }

    // ---------------- Anniversary CRUD ----------------

    fun upsertAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            if (anniversary.id == 0L) {
                repository.insertAnniversary(anniversary)
            } else {
                repository.updateAnniversary(anniversary)
            }
            // TODO：后续可在此处补充基于 Anniversary 的任务生成逻辑
            generateFutureTaskRecords(daysAhead = 30)
        }
    }

    fun deleteAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.deleteAnniversary(anniversary)
            // TODO：后续可在此处补充基于 Anniversary 的任务生成逻辑
            generateFutureTaskRecords(daysAhead = 30)
        }
    }

    // ---------------- Post CRUD（Feed 手账）----------------

    fun addPost(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val post = Post(
                createTime = System.currentTimeMillis(),
                content = content.trim(),
                imagePaths = null,
                linkedTaskIds = null
            )
            repository.insertPost(post)
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            repository.deletePost(post)
        }
    }
    
    // ---------------- Holiday Sync ----------------
    
    // 节假日同步状态
    private val _holidaySyncState = MutableStateFlow<HolidaySyncState>(HolidaySyncState.Idle)
    val holidaySyncState: StateFlow<HolidaySyncState> = _holidaySyncState.asStateFlow()
    
    /**
     * 同步节假日数据（当前年份及前后各一年）
     */
    fun syncHolidays() {
        viewModelScope.launch {
            _holidaySyncState.value = HolidaySyncState.Syncing
            try {
                val result = holidaySyncService.syncCurrentYearRange()
                _holidaySyncState.value = if (result.success) {
                    HolidaySyncState.Success(result.message, result.importedCount)
                } else {
                    HolidaySyncState.Error(result.message, result.error)
                }
                
                // 同步成功后，重新生成任务记录（因为节假日可能影响排班）
                if (result.success) {
                    generateFutureTaskRecords(daysAhead = 30)
                }
            } catch (e: Exception) {
                val errorMsg = "同步节假日数据失败: ${e.message}"
                FileLogger.e("MainViewModel", errorMsg, e)
                _holidaySyncState.value = HolidaySyncState.Error(errorMsg, e)
            }
        }
    }
    
    /**
     * 重置同步状态
     */
    fun resetHolidaySyncState() {
        _holidaySyncState.value = HolidaySyncState.Idle
    }
}

/**
 * 节假日同步状态
 */
sealed class HolidaySyncState {
    object Idle : HolidaySyncState()
    object Syncing : HolidaySyncState()
    data class Success(val message: String, val importedCount: Int) : HolidaySyncState()
    data class Error(val message: String, val error: Throwable?) : HolidaySyncState()
}

// Feed 信息流模型
data class FeedItem(
    val id: String,
    val type: FeedItemType,
    val timestamp: Long,
    val post: Post? = null,
    val task: TaskRecord? = null
)

enum class FeedItemType {
    POST,
    TASK
}

