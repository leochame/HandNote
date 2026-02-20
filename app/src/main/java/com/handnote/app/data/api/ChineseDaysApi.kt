package com.handnote.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Chinese Days API 接口
 * 数据源：https://cdn.jsdelivr.net/npm/chinese-days/dist/years/{year}.json
 */
interface ChineseDaysApi {
    /**
     * 获取指定年份的节假日数据
     * @param year 年份，如 2024, 2025
     */
    @GET("npm/chinese-days/dist/years/{year}.json")
    suspend fun getHolidaysByYear(@Path("year") year: Int): ChineseDaysResponse
}

