package com.handnote.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.handnote.app.ui.viewmodel.EmailItem
import com.handnote.app.ui.viewmodel.GmailUiState
import com.handnote.app.ui.viewmodel.GmailViewModel
import com.handnote.app.ui.viewmodel.InterviewInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailScreen(
    viewModel: GmailViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val gmailState by viewModel.gmailState.collectAsState()
    val signedInAccount by viewModel.signedInAccount.collectAsState()
    val emails by viewModel.emails.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val interviewItems by viewModel.interviewItems.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
        viewModel.onSignInSuccess(account)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSignedInState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "邮件助手",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "设置")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            signedInAccount == null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Mail,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "登录 Gmail 以浏览邮件",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = {
                                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                                    context,
                                    viewModel.getSignInOptions()
                                )
                                signInLauncher.launch(client.signInIntent)
                            }
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("登录 Gmail")
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已登录: ${signedInAccount?.email?.take(20) ?: ""}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.signOut() }) {
                        Text("退出登录")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.loadTodayEmailsAndSummarize() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gmailState !is GmailUiState.Loading
                ) {
                    if (gmailState is GmailUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取今日未读并 AI 总结")
                }

                when (val state = gmailState) {
                    is GmailUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    else -> {}
                }

                summary?.let { s ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "今日邮件总结",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (interviewItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "面试安排（已自动添加提醒）",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    interviewItems.forEach { info ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (info.company.isNotBlank()) {
                                    Text(
                                        text = info.company,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (info.position.isNotBlank()) {
                                    Text(
                                        text = info.position,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    if (info.date.isNotBlank()) Text("${info.date} ", style = MaterialTheme.typography.bodySmall)
                                    if (info.time.isNotBlank()) Text(info.time, style = MaterialTheme.typography.bodySmall)
                                }
                                if (info.location.isNotBlank()) {
                                    Text(
                                        text = info.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                if (emails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "邮件列表 (${emails.size} 封)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(emails) { email ->
                            EmailCard(email = email)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailCard(email: EmailItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = email.subject,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = email.from,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (email.snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.snippet.take(150) + if (email.snippet.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

