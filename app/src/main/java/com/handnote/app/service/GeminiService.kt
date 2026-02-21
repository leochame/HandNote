package com.handnote.app.service

import android.util.Log
import com.handnote.app.data.api.GeminiApi
import com.handnote.app.data.api.GeminiContent
import com.handnote.app.data.api.GeminiGenerateRequest
import com.handnote.app.data.api.GeminiPart
import com.handnote.app.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Gemini AI 服务：邮件总结与面试信息提取
 */
class GeminiService(private val apiKey: String?) {

    companion object {
        private const val TAG = "GeminiService"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    /**
     * 总结今日未读邮件
     */
    suspend fun summarizeEmails(emailContents: List<String>): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(Exception("请先在设置中配置 Gemini API Key"))
        }
        if (emailContents.isEmpty()) {
            return@withContext Result.success("今日暂无未读邮件。")
        }

        val combined = emailContents.take(20).joinToString("\n\n---\n\n") { it.take(2000) }
        val prompt = """
            请用简洁的中文总结以下今日未读邮件的内容要点，每条邮件用一两句话概括。
            如果邮件较多，可以按重要性或类型分组说明。
            
            邮件内容：
            $combined
        """.trimIndent()

        return@withContext callGemini(prompt)
    }

    /**
     * 从邮件内容中提取面试安排，返回结构化数据
     */
    suspend fun extractInterviewInfo(emailContents: List<String>): Result<List<InterviewInfo>> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(Exception("请先在设置中配置 Gemini API Key"))
        }
        if (emailContents.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val combined = emailContents.take(15).joinToString("\n\n---\n\n") { it.take(1500) }
        val prompt = """
            分析以下邮件内容，找出所有与「面试」相关的安排信息。
            包括：面试邀请、面试时间确认、视频面试链接等。
            
            对每条面试安排，提取并返回 JSON 数组，每个元素包含：
            - company: 公司名称
            - position: 职位名称（如有）
            - date: 日期，格式 YYYY-MM-DD
            - time: 具体时间，如 "14:00" 或 "下午2点"
            - location: 地点或视频链接
            - notes: 其他备注
            
            如果没有找到任何面试安排，返回空数组 []。
            只返回 JSON 数组，不要其他说明文字。
            
            邮件内容：
            $combined
        """.trimIndent()

        val result = callGemini(prompt)
        result.fold(
            onSuccess = { text ->
                try {
                    val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val arr = JSONArray(cleaned)
                    val list = mutableListOf<InterviewInfo>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            InterviewInfo(
                                company = obj.optString("company", ""),
                                position = obj.optString("position", ""),
                                date = obj.optString("date", ""),
                                time = obj.optString("time", ""),
                                location = obj.optString("location", ""),
                                notes = obj.optString("notes", "")
                            )
                        )
                    }
                    Result.success(list.filter { it.company.isNotBlank() || it.date.isNotBlank() })
                } catch (e: Exception) {
                    FileLogger.e(TAG, "Failed to parse interview JSON", e)
                    Result.success(emptyList())
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun callGemini(prompt: String): Result<String> {
        return try {
            val request = GeminiGenerateRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = com.handnote.app.data.api.GeminiGenerationConfig(
                    temperature = 0.3,
                    maxOutputTokens = 2048
                )
            )
            val response = api.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    Result.success(text)
                } else {
                    val err = body?.error?.message ?: "Empty response"
                    Result.failure(Exception(err))
                }
            } else {
                val errBody = response.errorBody()?.string() ?: "Unknown error"
                FileLogger.e(TAG, "Gemini API error: ${response.code()} - $errBody")
                Result.failure(Exception("AI 请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            FileLogger.e(TAG, "Gemini call failed", e)
            Result.failure(e)
        }
    }
}

data class InterviewInfo(
    val company: String,
    val position: String,
    val date: String,
    val time: String,
    val location: String,
    val notes: String
)

