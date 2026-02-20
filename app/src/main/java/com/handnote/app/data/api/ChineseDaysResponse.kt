package com.handnote.app.data.api

/**
 * Chinese Days API 响应数据模型
 * 参考：https://github.com/vsme/chinese-days
 *
 * 实际 API 返回格式：
 * {
 *   "holidays": { "2025-01-01": "New Year's Day,元旦,1", ... },
 *   "workdays": { "2025-01-26": "Spring Festival,春节,4", ... },
 *   "inLieuDays": { ... }
 * }
 */
data class ChineseDaysResponse(
    val holidays: Map<String, String>?, // 日期 -> "英文名,中文名,天数"
    val workdays: Map<String, String>?, // 调休工作日
    val inLieuDays: Map<String, String>? // 补休日
)

/**
 * 解析后的节假日数据
 */
data class ParsedHoliday(
    val date: String,
    val englishName: String,
    val chineseName: String,
    val days: Int
) {
    companion object {
        /**
         * 从 API 返回的字符串解析节假日信息
         * 格式: "English Name,中文名,天数"
         */
        fun parse(date: String, value: String): ParsedHoliday? {
            val parts = value.split(",")
            return if (parts.size >= 3) {
                ParsedHoliday(
                    date = date,
                    englishName = parts[0],
                    chineseName = parts[1],
                    days = parts[2].toIntOrNull() ?: 1
                )
            } else {
                null
            }
        }
    }
}

