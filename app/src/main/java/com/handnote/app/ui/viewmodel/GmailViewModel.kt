package com.handnote.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.handnote.app.data.entity.TaskRecord
import com.handnote.app.data.repository.AppRepository
import com.handnote.app.service.AlarmManagerService
import com.handnote.app.service.GeminiService
import com.handnote.app.service.GmailService
import com.handnote.app.service.InterviewInfo
import com.handnote.app.util.GmailPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GmailViewModel(
    private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    private val gmailService = GmailService(context)
    private val alarmManagerService = AlarmManagerService(context)

    private val _gmailState = MutableStateFlow<GmailUiState>(GmailUiState.Idle)
    val gmailState: StateFlow<GmailUiState> = _gmailState.asStateFlow()

    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()

    private val _emails = MutableStateFlow<List<EmailItem>>(emptyList())
    val emails: StateFlow<List<EmailItem>> = _emails.asStateFlow()

    private val _summary = MutableStateFlow<String?>(null)
    val summary: StateFlow<String?> = _summary.asStateFlow()

    private val _interviewItems = MutableStateFlow<List<InterviewInfo>>(emptyList())
    val interviewItems: StateFlow<List<InterviewInfo>> = _interviewItems.asStateFlow()

    init {
        refreshSignedInState()
    }

    fun refreshSignedInState() {
        _signedInAccount.value = gmailService.getSignedInAccount()
    }

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .build()
    }

    fun onSignInSuccess(account: GoogleSignInAccount?) {
        _signedInAccount.value = account
    }

    fun signOut() {
        GoogleSignIn.getClient(context, getSignInOptions()).signOut()
        _signedInAccount.value = null
        _emails.value = emptyList()
        _summary.value = null
        _interviewItems.value = emptyList()
        _gmailState.value = GmailUiState.Idle
    }

    fun loadTodayEmailsAndSummarize() {
        viewModelScope.launch {
            _gmailState.value = GmailUiState.Loading
            val account = gmailService.getSignedInAccount()
            if (account == null) {
                _gmailState.value = GmailUiState.Error("请先登录 Gmail 账户")
                return@launch
            }

            val geminiKey = GmailPrefs.getGeminiApiKey(context)
            if (geminiKey.isNullOrBlank()) {
                _gmailState.value = GmailUiState.Error("请先在设置中配置 Gemini API Key")
                return@launch
            }

            val result = gmailService.getTodayUnreadMessages()
            result.fold(
                onSuccess = { messages ->
                    val items = messages.map { msg ->
                        EmailItem(
                            id = msg.id,
                            subject = gmailService.getSubject(msg),
                            from = gmailService.getFrom(msg),
                            snippet = msg.snippet ?: "",
                            fullContent = gmailService.extractPlainText(msg)
                        )
                    }
                    _emails.value = items

                    if (items.isEmpty()) {
                        _summary.value = "今日暂无未读邮件。"
                        _interviewItems.value = emptyList()
                        _gmailState.value = GmailUiState.Success
                        return@launch
                    }

                    val geminiService = GeminiService(geminiKey)
                    val contents = items.map { it.fullContent }

                    // 并行：总结 + 提取面试
                    val summaryResult = geminiService.summarizeEmails(contents)
                    val interviewResult = geminiService.extractInterviewInfo(contents)

                    summaryResult.onSuccess { _summary.value = it }
                    summaryResult.onFailure { _summary.value = "AI 总结失败: ${it.message}" }

                    interviewResult.onSuccess { interviews ->
                        _interviewItems.value = interviews
                        if (interviews.isNotEmpty()) {
                            createInterviewTasks(interviews)
                        }
                    }

                    _gmailState.value = GmailUiState.Success
                },
                onFailure = {
                    _gmailState.value = GmailUiState.Error(it.message ?: "加载失败")
                }
            )
        }
    }

    private fun createInterviewTasks(interviews: List<InterviewInfo>) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            for (info in interviews) {
                val dateStr = info.date.ifBlank { LocalDate.now().format(formatter) }
                val timeStr = info.time.ifBlank { "09:00" }
                val triggerTime = try {
                    val date = LocalDate.parse(dateStr, formatter)
                    val time = parseTime(timeStr)
                    date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    LocalDate.now().atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                val title = buildString {
                    append("面试")
                    if (info.company.isNotBlank()) append(" - ${info.company}")
                    if (info.position.isNotBlank()) append(" ${info.position}")
                }

                val task = TaskRecord(
                    sourceType = "gmail_interview",
                    sourceId = 0L,
                    targetDate = dateStr,
                    triggerTimestamp = triggerTime,
                    reminderLevel = 1,
                    status = "pending",
                    targetPkgName = null
                )
                repository.insertTaskRecord(task)
            }
            alarmManagerService.registerAllPendingTasks(repository)
        }
    }

    private fun parseTime(s: String): LocalTime {
        return try {
            val cleaned = s.replace("下午", "").replace("上午", "").replace("点", ":").trim()
            when {
                cleaned.contains(":") -> LocalTime.parse(cleaned.take(5), DateTimeFormatter.ofPattern("HH:mm"))
                cleaned.length <= 2 -> LocalTime.of(cleaned.toIntOrNull() ?: 9, 0)
                else -> LocalTime.of(9, 0)
            }
        } catch (e: Exception) {
            LocalTime.of(9, 0)
        }
    }

    fun saveGeminiApiKey(key: String?) {
        GmailPrefs.setGeminiApiKey(context, key)
    }

    fun getGeminiApiKey(): String? = GmailPrefs.getGeminiApiKey(context)
}

sealed class GmailUiState {
    object Idle : GmailUiState()
    object Loading : GmailUiState()
    object Success : GmailUiState()
    data class Error(val message: String) : GmailUiState()
}

data class EmailItem(
    val id: String,
    val subject: String,
    val from: String,
    val snippet: String,
    val fullContent: String
)

