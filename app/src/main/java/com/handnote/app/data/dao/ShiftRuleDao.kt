package com.handnote.app.data.dao

import androidx.room.*
import com.handnote.app.data.entity.ShiftRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftRuleDao {
    @Query("SELECT * FROM shift_rules ORDER BY startDate DESC")
    fun getAllShiftRules(): Flow<List<ShiftRule>>

    @Query("SELECT * FROM shift_rules WHERE id = :id")
    suspend fun getShiftRuleById(id: Long): ShiftRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftRule(shiftRule: ShiftRule): Long

    @Update
    suspend fun updateShiftRule(shiftRule: ShiftRule)

    @Delete
    suspend fun deleteShiftRule(shiftRule: ShiftRule)

    @Query("DELETE FROM shift_rules WHERE id = :id")
    suspend fun deleteShiftRuleById(id: Long)
}

