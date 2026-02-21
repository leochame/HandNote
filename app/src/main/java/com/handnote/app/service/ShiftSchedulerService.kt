package com.handnote.app.service

import com.handnote.app.data.entity.ShiftRule
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

/**
 * 排班配置中的单日配置
 */
data class DayShiftConfig(
    val dayIndex: Int, // 在周期中的第几天（0-based）
    val timeSlots: List<TimeSlotConfig>, // 该天的多个时间点
    val reminderLevel: Int = 2 // 默认强提醒
)

/**
 * 时间点配置
 */
data class TimeSlotConfig(
    val time: String, // 时间格式 "HH:mm"
    val targetPkgName: String? = null // 目标应用包名
)

/**
 * 排班调度服务
 * 负责根据排班规则生成 TaskRecord
 */
class ShiftSchedulerService(
    private val repository: AppRepository
) {
    /**
     * 根据排班规则生成指定日期范围内的任务记录
     * 
     * @param shiftRule 排班规则
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 生成的任务记录列表
     */
    suspend fun generateTaskRecords(
        shiftRule: ShiftRule,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TaskRecord> {
        val taskRecords = mutableListOf<TaskRecord>()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        // 解析排班配置 JSON
        val dayConfigs = parseShiftConfig(shiftRule.shiftConfig)
        
        // 获取节假日列表（如果需要跳过节假日）
        val holidays = if (shiftRule.skipHoliday) {
            try {
            repository.getAllHolidays().first()
                .filter { it.type == "1" } // 只考虑法定休息日（type 为 "1"）
                    .mapNotNull { holiday ->
                        try {
                            LocalDate.parse(holiday.date, formatter)
                        } catch (e: Exception) {
                            null // 跳过无效日期
                        }
                    }
                .toSet()
            } catch (e: Exception) {
                emptySet() // 如果获取失败，返回空集合
            }
        } else {
            emptySet()
        }
        
        // 计算基准日期
        val baseDate = LocalDate.ofEpochDay(shiftRule.startDate / 86400000)
        
        // 遍历日期范围
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            // 跳过节假日（如果需要）
            if (shiftRule.skipHoliday && holidays.contains(currentDate)) {
                currentDate = currentDate.plusDays(1)
                continue
            }
            
            // 计算当前日期在周期中的索引
            val dayIndex = calculateDayIndex(baseDate, currentDate, shiftRule.cycleDays)
            
            // 查找该索引对应的排班配置
            val dayConfig = dayConfigs.find { it.dayIndex == dayIndex }
            
            if (dayConfig != null) {
                // 为该天的每个时间点生成任务记录
                dayConfig.timeSlots.forEach { timeSlot ->
                    try {
                    val triggerTimestamp = parseTimeToTimestamp(currentDate, timeSlot.time)
                    
                    val taskRecord = TaskRecord(
                        sourceType = "shift_rule",
                        sourceId = shiftRule.id,
                        targetDate = currentDate.format(formatter),
                        triggerTimestamp = triggerTimestamp,
                        reminderLevel = dayConfig.reminderLevel,
                        status = "pending",
                        targetPkgName = timeSlot.targetPkgName
                    )
                    
                    taskRecords.add(taskRecord)
                    } catch (e: Exception) {
                        // 跳过无效的时间格式，避免整个任务生成失败
                        // 可以在这里添加日志记录
                    }
                }
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return taskRecords
    }
    
    /**
     * 计算日期在周期中的索引
     * 公式：DayIndex = (CurrentDate - StartDate) % CycleDays
     */
    private fun calculateDayIndex(
        baseDate: LocalDate,
        currentDate: LocalDate,
        cycleDays: Int
    ): Int {
        val daysDiff = currentDate.toEpochDay() - baseDate.toEpochDay()
        // 处理负数情况（当前日期在基准日期之前）
        val normalizedDiff = if (daysDiff < 0) {
            daysDiff + cycleDays * ((Math.abs(daysDiff) / cycleDays) + 1)
        } else {
            daysDiff
        }
        return (normalizedDiff % cycleDays).toInt()
    }
    
    /**
     * 解析排班配置 JSON
     * 格式示例：
     * [
     *   {
     *     "dayIndex": 0,
     *     "reminderLevel": 2,
     *     "timeSlots": [
     *       {"time": "08:00", "targetPkgName": "com.example.app"},
     *       {"time": "20:00", "targetPkgName": "com.example.app"}
     *     ]
     *   },
     *   {
     *     "dayIndex": 1,
     *     "reminderLevel": 2,
     *     "timeSlots": [
     *       {"time": "08:00", "targetPkgName": "com.example.app"}
     *     ]
     *   }
     * ]
     */
    private fun parseShiftConfig(jsonString: String): List<DayShiftConfig> {
        val configs = mutableListOf<DayShiftConfig>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val dayObj = jsonArray.getJSONObject(i)
                val dayIndex = dayObj.getInt("dayIndex")
                val reminderLevel = dayObj.optInt("reminderLevel", 2)
                
                val timeSlots = mutableListOf<TimeSlotConfig>()
                val timeSlotsArray = dayObj.getJSONArray("timeSlots")
                for (j in 0 until timeSlotsArray.length()) {
                    val slotObj = timeSlotsArray.getJSONObject(j)
                    val time = slotObj.getString("time")
                    val targetPkgName = slotObj.optString("targetPkgName", "")
                        .takeIf { it.isNotEmpty() }
                    
                    timeSlots.add(TimeSlotConfig(time, targetPkgName))
                }
                
                configs.add(DayShiftConfig(dayIndex, timeSlots, reminderLevel))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 返回空配置，避免崩溃
        }
        
        return configs
    }
    
    /**
     * 将日期和时间字符串转换为时间戳
     */
    private fun parseTimeToTimestamp(date: LocalDate, time: String): Long {
        val timeParts = time.split(":")
        if (timeParts.isEmpty()) {
            throw IllegalArgumentException("Invalid time format: $time")
        }
        val hour = timeParts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid hour: ${timeParts[0]}")
        val minute = timeParts.getOrElse(1) { "0" }.toIntOrNull() ?: throw IllegalArgumentException("Invalid minute: ${timeParts.getOrElse(1) { "0" }}")
        
        val dateTime = date.atTime(hour, minute)
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
    }
    
    /**
     * 为所有排班规则与纪念日生成未来 N 天的任务记录
     */
    suspend fun generateAllTaskRecords(daysAhead: Int = 30) {
        val today = LocalDate.now()
        val endDate = today.plusDays(daysAhead.toLong())

        // 1. 基于排班规则生成任务
        val shiftRules = try {
            repository.getAllShiftRules().first()
        } catch (e: Exception) {
            emptyList() // 如果获取失败，返回空列表
        }

        for (rule in shiftRules) {
            // 先清理该规则的旧任务记录（仅清理 pending 状态的）
            repository.deletePendingTasksBySource("shift_rule", rule.id)

            val taskRecords = generateTaskRecords(rule, today, endDate)

            // 批量插入任务记录（避免重复）
            for (taskRecord in taskRecords) {
                // 检查是否已存在相同的任务记录
                val existing = repository.getTaskRecordsByDate(taskRecord.targetDate)
                    .find {
                        it.sourceType == taskRecord.sourceType &&
                        it.sourceId == taskRecord.sourceId &&
                        it.triggerTimestamp == taskRecord.triggerTimestamp
                    }

                if (existing == null) {
                    repository.insertTaskRecord(taskRecord)
                }
            }
        }

        // 2. 基于纪念日生成任务（按年循环）
        generateAnniversaryTaskRecords(today, endDate)
    }

    /**
     * 为纪念日生成未来一段时间内的 TaskRecord
     * 纪念日按年循环：仅匹配月份与日期，忽略年份
     */
    private suspend fun generateAnniversaryTaskRecords(
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val anniversaries = try {
            repository.getAllAnniversaries().first()
        } catch (e: Exception) {
            emptyList() // 如果获取失败，返回空列表
        }

        if (anniversaries.isEmpty()) return

        // 为避免跨年遗漏，最多检查当前年和下一年
        val yearsToCheck = listOf(startDate.year, startDate.year + 1)

        for (anniversary in anniversaries) {
            // 先清理该纪念日的旧任务记录（仅清理 pending 状态的）
            repository.deletePendingTasksBySource("anniversary", anniversary.id)

            runCatching {
                val originalDate = LocalDate.parse(anniversary.targetDate, formatter)
                val monthDay = MonthDay.of(originalDate.month, originalDate.dayOfMonth)

                for (year in yearsToCheck) {
                    val occurrenceDate = monthDay.atYear(year)

                    // 仅生成在目标时间窗口内的实例
                    if (occurrenceDate.isBefore(startDate) || occurrenceDate.isAfter(endDate)) {
                        continue
                    }

                    val triggerTimestamp = buildAnniversaryTriggerTimestamp(
                        occurrenceDate,
                        anniversary.reminderTime
                    )

                    val taskRecord = TaskRecord(
                        sourceType = "anniversary",
                        sourceId = anniversary.id,
                        targetDate = occurrenceDate.format(formatter),
                        triggerTimestamp = triggerTimestamp,
                        reminderLevel = anniversary.reminderLevel,
                        status = "pending",
                        targetPkgName = null
                    )

                    // 避免重复插入同一纪念日实例
                    val existing = repository.getTaskRecordsByDate(taskRecord.targetDate)
                        .find {
                            it.sourceType == taskRecord.sourceType &&
                            it.sourceId == taskRecord.sourceId &&
                            it.triggerTimestamp == taskRecord.triggerTimestamp
                        }

                    if (existing == null) {
                        repository.insertTaskRecord(taskRecord)
                    }
                }
            }.onFailure {
                // 单条纪念日数据异常不影响整体生成
                it.printStackTrace()
            }
        }
    }

    /**
     * 根据纪念日配置构造触发时间戳：
     * - 若 `reminderTime` 不为空，则只取其中的时分信息
     * - 否则默认设置为当天 09:00
     */
    private fun buildAnniversaryTriggerTimestamp(
        date: LocalDate,
        reminderTime: Long?
    ): Long {
        val time: LocalTime = if (reminderTime != null) {
            val instant = Instant.ofEpochMilli(reminderTime)
            val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            localDateTime.toLocalTime()
        } else {
            LocalTime.of(9, 0)
        }

        val dateTime = date.atTime(time.hour, time.minute)
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
    }
}

