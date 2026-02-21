package com.handnote.app.data.repository

import com.handnote.app.data.dao.*
import com.handnote.app.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val shiftRuleDao: ShiftRuleDao,
    private val anniversaryDao: AnniversaryDao,
    private val taskRecordDao: TaskRecordDao,
    private val postDao: PostDao,
    private val holidayDao: HolidayDao
) {
    // ShiftRule operations
    fun getAllShiftRules(): Flow<List<ShiftRule>> = shiftRuleDao.getAllShiftRules()
    suspend fun getShiftRuleById(id: Long) = shiftRuleDao.getShiftRuleById(id)
    suspend fun insertShiftRule(shiftRule: ShiftRule) = shiftRuleDao.insertShiftRule(shiftRule)
    suspend fun updateShiftRule(shiftRule: ShiftRule) = shiftRuleDao.updateShiftRule(shiftRule)
    suspend fun deleteShiftRule(shiftRule: ShiftRule) = shiftRuleDao.deleteShiftRule(shiftRule)

    // Anniversary operations
    fun getAllAnniversaries(): Flow<List<Anniversary>> = anniversaryDao.getAllAnniversaries()
    suspend fun getAnniversaryById(id: Long) = anniversaryDao.getAnniversaryById(id)
    suspend fun getAnniversariesByDate(date: String) = anniversaryDao.getAnniversariesByDate(date)
    suspend fun insertAnniversary(anniversary: Anniversary) = anniversaryDao.insertAnniversary(anniversary)
    suspend fun updateAnniversary(anniversary: Anniversary) = anniversaryDao.updateAnniversary(anniversary)
    suspend fun deleteAnniversary(anniversary: Anniversary) = anniversaryDao.deleteAnniversary(anniversary)

    // TaskRecord operations
    fun getAllTaskRecords(): Flow<List<TaskRecord>> = taskRecordDao.getAllTaskRecords()
    suspend fun getTaskRecordById(id: Long) = taskRecordDao.getTaskRecordById(id)
    suspend fun getTaskRecordsByDate(date: String) = taskRecordDao.getTaskRecordsByDate(date)
    fun getTaskRecordsByDateFlow(date: String): Flow<List<TaskRecord>> = taskRecordDao.getTaskRecordsByDateFlow(date)
    suspend fun insertTaskRecord(taskRecord: TaskRecord) = taskRecordDao.insertTaskRecord(taskRecord)
    suspend fun updateTaskRecord(taskRecord: TaskRecord) = taskRecordDao.updateTaskRecord(taskRecord)
    suspend fun deleteTaskRecord(taskRecord: TaskRecord) = taskRecordDao.deleteTaskRecord(taskRecord)
    suspend fun deletePendingTasksBySource(sourceType: String, sourceId: Long) = taskRecordDao.deletePendingTasksBySource(sourceType, sourceId)

    // Post operations
    fun getAllPosts(): Flow<List<Post>> = postDao.getAllPosts()
    suspend fun getPostById(id: Long) = postDao.getPostById(id)
    fun getRecentPosts(limit: Int): Flow<List<Post>> = postDao.getRecentPosts(limit)
    suspend fun insertPost(post: Post) = postDao.insertPost(post)
    suspend fun updatePost(post: Post) = postDao.updatePost(post)
    suspend fun deletePost(post: Post) = postDao.deletePost(post)

    // Holiday operations
    fun getAllHolidays(): Flow<List<Holiday>> = holidayDao.getAllHolidays()
    suspend fun getHolidayByDate(date: String) = holidayDao.getHolidayByDate(date)
    suspend fun getHolidaysBetweenDates(startDate: String, endDate: String) = holidayDao.getHolidaysBetweenDates(startDate, endDate)
    suspend fun insertHoliday(holiday: Holiday) = holidayDao.insertHoliday(holiday)
    suspend fun insertHolidays(holidays: List<Holiday>) = holidayDao.insertHolidays(holidays)
    suspend fun updateHoliday(holiday: Holiday) = holidayDao.updateHoliday(holiday)
    suspend fun deleteHoliday(holiday: Holiday) = holidayDao.deleteHoliday(holiday)
}

