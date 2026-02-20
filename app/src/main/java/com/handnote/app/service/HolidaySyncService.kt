package com.handnote.app.service

import android.util.Log
import com.handnote.app.data.api.ChineseDaysApi
import com.handnote.app.data.api.ParsedHoliday
import com.handnote.app.data.entity.Holiday
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

/**
 * 节假日数据同步服务
 * 从 Chinese Days API 获取节假日数据并同步到本地数据库
 */
class HolidaySyncService(
    private val repository: AppRepository
) {
    companion object {
        private const val TAG = "HolidaySyncService"
        private const val BASE_URL = "https://cdn.jsdelivr.net/"

        // 数据源：Chinese Days
        // https://cdn.jsdelivr.net/npm/chinese-days/dist/years/{year}.json
    }

    private val api: ChineseDaysApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChineseDaysApi::class.java)
    }

    /**
     * 同步指定年份的节假日数据
     * @param year 年份
     * @return 同步结果（成功导入的节假日数量）
     */
    suspend fun syncHolidaysForYear(year: Int): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始同步 $year 年的节假日数据...")
            FileLogger.d(TAG, "开始同步 $year 年的节假日数据...")

            // 从 API 获取数据
            val response = api.getHolidaysByYear(year)

            // 检查响应数据是否有效
            if (response.holidays.isNullOrEmpty()) {
                val message = "$year 年没有找到节假日数据（API 返回空数据）"
                Log.w(TAG, message)
                FileLogger.w(TAG, message)
                return@withContext SyncResult(
                    success = false,
                    message = message,
                    importedCount = 0
                )
            }

            // 解析节假日数据
            val holidays = mutableListOf<Holiday>()

            // 解析 holidays（法定节假日）
            response.holidays.forEach { (date, value) ->
                ParsedHoliday.parse(date, value)?.let { parsed ->
                    holidays.add(
                        Holiday(
                            date = parsed.date,
                            type = "holiday",
                            name = parsed.chineseName
                        )
                    )
                }
            }

            // 解析 workdays（调休工作日）
            response.workdays?.forEach { (date, value) ->
                ParsedHoliday.parse(date, value)?.let { parsed ->
                    holidays.add(
                        Holiday(
                            date = parsed.date,
                            type = "workday",
                            name = "${parsed.chineseName}调休"
                        )
                    )
                }
            }

            if (holidays.isEmpty()) {
                Log.w(TAG, "$year 年没有找到节假日数据")
                FileLogger.w(TAG, "$year 年没有找到节假日数据")
                return@withContext SyncResult(
                    success = false,
                    message = "$year 年没有找到节假日数据",
                    importedCount = 0
                )
            }

            // 批量插入到数据库（使用 insertHolidays，Room 会自动处理冲突）
            repository.insertHolidays(holidays)

            Log.d(TAG, "成功同步 $year 年节假日数据，共 ${holidays.size} 条")
            FileLogger.d(TAG, "成功同步 $year 年节假日数据，共 ${holidays.size} 条")

            SyncResult(
                success = true,
                message = "成功同步 $year 年节假日数据，共 ${holidays.size} 条",
                importedCount = holidays.size
            )
        } catch (e: HttpException) {
            // 处理 HTTP 错误（如 404）
            val errorMsg = when (e.code()) {
                404 -> "同步 $year 年节假日数据失败: HTTP 404（该年份数据尚未发布）"
                else -> "同步 $year 年节假日数据失败: HTTP ${e.code()} ${e.message()}"
            }
            Log.w(TAG, errorMsg)
            FileLogger.w(TAG, errorMsg)

            SyncResult(
                success = false,
                message = errorMsg,
                importedCount = 0,
                error = e
            )
        } catch (e: Exception) {
            val errorMsg = "同步 $year 年节假日数据失败: ${e.message}"
            Log.e(TAG, errorMsg, e)
            FileLogger.e(TAG, errorMsg, e)

            SyncResult(
                success = false,
                message = errorMsg,
                importedCount = 0,
                error = e
            )
        }
    }

    /**
     * 同步当前年份及前后各一年的节假日数据
     * @return 同步结果
     */
    suspend fun syncCurrentYearRange(): SyncResult {
        val currentYear = LocalDate.now().year
        val yearsToSync = listOf(currentYear - 1, currentYear, currentYear + 1)

        var totalImported = 0
        val errors = mutableListOf<String>()

        yearsToSync.forEach { year ->
            val result = syncHolidaysForYear(year)
            if (result.success) {
                totalImported += result.importedCount
            } else {
                errors.add(result.message)
            }
        }

        return if (errors.isEmpty()) {
            SyncResult(
                success = true,
                message = "成功同步 ${yearsToSync.joinToString(", ")} 年节假日数据，共 $totalImported 条",
                importedCount = totalImported
            )
        } else {
            SyncResult(
                success = totalImported > 0,
                message = if (totalImported > 0) {
                    "部分同步成功（$totalImported 条），部分失败：${errors.joinToString("; ")}"
                } else {
                    "同步失败：${errors.joinToString("; ")}"
                },
                importedCount = totalImported
            )
        }
    }

    /**
     * 同步结果
     */
    data class SyncResult(
        val success: Boolean,
        val message: String,
        val importedCount: Int,
        val error: Throwable? = null
    )
}

