package com.handnote.app.data.dao

import androidx.room.*
import com.handnote.app.data.entity.TaskRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskRecordDao {
    @Query("SELECT * FROM task_records ORDER BY targetDate DESC, triggerTimestamp DESC")
    fun getAllTaskRecords(): Flow<List<TaskRecord>>

    @Query("SELECT * FROM task_records WHERE id = :id")
    suspend fun getTaskRecordById(id: Long): TaskRecord?

    @Query("SELECT * FROM task_records WHERE targetDate = :date ORDER BY triggerTimestamp ASC")
    suspend fun getTaskRecordsByDate(date: String): List<TaskRecord>

    @Query("SELECT * FROM task_records WHERE targetDate = :date ORDER BY triggerTimestamp ASC")
    fun getTaskRecordsByDateFlow(date: String): Flow<List<TaskRecord>>

    @Query("SELECT * FROM task_records WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun getTaskRecordsBySource(sourceType: String, sourceId: Long): List<TaskRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskRecord(taskRecord: TaskRecord): Long

    @Update
    suspend fun updateTaskRecord(taskRecord: TaskRecord)

    @Delete
    suspend fun deleteTaskRecord(taskRecord: TaskRecord)

    @Query("DELETE FROM task_records WHERE id = :id")
    suspend fun deleteTaskRecordById(id: Long)

    @Query("DELETE FROM task_records WHERE sourceType = :sourceType AND sourceId = :sourceId AND status = 'pending'")
    suspend fun deletePendingTasksBySource(sourceType: String, sourceId: Long)
}

