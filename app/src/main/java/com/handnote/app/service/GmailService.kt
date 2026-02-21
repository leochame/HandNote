package com.handnote.app.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.handnote.app.data.api.GmailApi
import com.handnote.app.data.api.GmailMessage
import com.handnote.app.data.api.GmailMessageListResponse
import com.handnote.app.data.api.GmailMessageRef
import com.handnote.app.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Gmail 服务：负责 OAuth 认证和邮件读取
 */
class GmailService(private val context: Context) {

    companion object {
        private const val TAG = "GmailService"
        private const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
        private const val GMAIL_BASE_URL = "https://gmail.googleapis.com/"
    }

    private val gmailApi: GmailApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = getAccessTokenBlocking()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .apply {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        Retrofit.Builder()
            .baseUrl(GMAIL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GmailApi::class.java)
    }

    /**
     * 获取当前已登录的 Gmail 账户
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GMAIL_SCOPE))
            .build()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && account.grantedScopes?.any {
            it.toString() == GMAIL_SCOPE
        } == true) account else null
    }

    /**
     * 获取 OAuth Access Token（需在后台线程调用）
     */
    private fun getAccessTokenBlocking(): String? {
        val account = getSignedInAccount() ?: return null
        return try {
            GoogleAuthUtil.getToken(context, account.account!!, "oauth2:$GMAIL_SCOPE")
        } catch (e: Exception) {
            FileLogger.e(TAG, "Failed to get access token", e)
            null
        }
    }

    /**
     * 获取今日未读邮件列表
     */
    suspend fun getTodayUnreadMessages(): Result<List<GmailMessage>> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
            if (account == null) {
                return@withContext Result.failure(Exception("请先登录 Gmail 账户"))
            }

            // 查询今日未读邮件 (Gmail 搜索语法)
            val today = java.time.LocalDate.now().toString().replace("-", "/")
            val query = "is:unread after:$today"
            val response = gmailApi.listMessages(query = query, maxResults = 50)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                FileLogger.e(TAG, "Gmail API error: ${response.code()} - $errorBody")
                return@withContext Result.failure(Exception("获取邮件失败: ${response.code()}"))
            }

            val body = response.body() ?: return@withContext Result.success(emptyList())
            val messageRefs = body.messages ?: emptyList()

            val messages = messageRefs.mapNotNull { ref ->
                try {
                    val msgResponse = gmailApi.getMessage(ref.id)
                    if (msgResponse.isSuccessful) msgResponse.body() else null
                } catch (e: Exception) {
                    FileLogger.e(TAG, "Failed to get message ${ref.id}", e)
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            FileLogger.e(TAG, "getTodayUnreadMessages failed", e)
            Result.failure(e)
        }
    }

    /**
     * 从 Gmail 消息中提取纯文本内容
     */
    fun extractPlainText(message: GmailMessage): String {
        val parts = message.payload?.parts ?: listOfNotNull(message.payload)
        val textParts = mutableListOf<String>()
        for (part in parts) {
            if (part.mimeType == "text/plain" && part.body?.data != null) {
                val decoded = decodeBase64Url(part.body.data)
                if (decoded.isNotBlank()) textParts.add(decoded)
            } else if (part.mimeType == "text/html" && part.body?.data != null && textParts.isEmpty()) {
                // 若无 plain，用 html 并简单去标签
                val html = decodeBase64Url(part.body.data)
                textParts.add(html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim())
            }
            part.parts?.let { nested ->
                for (p in nested) {
                    if (p.mimeType == "text/plain" && p.body?.data != null) {
                        textParts.add(decodeBase64Url(p.body.data))
                    }
                }
            }
        }
        if (textParts.isEmpty() && message.snippet != null) {
            textParts.add(message.snippet)
        }
        return textParts.joinToString("\n\n").take(8000) // 限制长度避免 API 超限
    }

    private fun decodeBase64Url(data: String): String {
        return try {
            val bytes = Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun getSubject(message: GmailMessage): String {
        return message.payload?.headers?.find { it.name.equals("Subject", true) }?.value ?: "(无主题)"
    }

    fun getFrom(message: GmailMessage): String {
        return message.payload?.headers?.find { it.name.equals("From", true) }?.value ?: ""
    }
}

