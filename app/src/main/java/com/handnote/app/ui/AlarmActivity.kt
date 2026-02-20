package com.handnote.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.handnote.app.service.AlarmForegroundService
import com.handnote.app.ui.theme.HandNoteTheme

/**
 * 全屏闹钟 Activity
 * 在锁屏状态下强制显示，用于强提醒（Level 2）
 */
class AlarmActivity : ComponentActivity() {

    private var taskId: Long = -1
    private var taskTitle: String = ""
    private var targetPkgName: String? = null
    private var targetAppName: String? = null
    private var targetAppIcon: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取传递的参数
        taskId = intent.getLongExtra("task_id", -1)
        taskTitle = intent.getStringExtra("task_title") ?: "提醒"
        targetPkgName = intent.getStringExtra("target_pkg_name")

        // 获取目标应用信息
        targetPkgName?.let { pkgName ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                targetAppName = packageManager.getApplicationLabel(appInfo).toString()
                targetAppIcon = packageManager.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Target app not found: $pkgName")
            }
        }

        // 设置全屏显示
        setContent {
            HandNoteTheme {
                AlarmScreen(
                    taskTitle = taskTitle,
                    targetPkgName = targetPkgName,
                    targetAppName = targetAppName,
                    targetAppIcon = targetAppIcon,
                    onDismiss = { finish() },
                    onOpenApp = { openTargetApp() }
                )
            }
        }
    }
    
    /**
     * 打开目标应用
     */
    private fun openTargetApp() {
        targetPkgName?.let { pkgName ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    // 应用未安装，跳转到应用市场
                    try {
                        val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=$pkgName")
                            setPackage("com.android.vending")
                        }
                        startActivity(marketIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open app market", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open target app: $pkgName", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止前台服务
        val stopIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_STOP
        }
        stopService(stopIntent)
    }
    
    companion object {
        private const val TAG = "AlarmActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    taskTitle: String,
    targetPkgName: String?,
    targetAppName: String?,
    targetAppIcon: Drawable?,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit
) {
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE53935)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⏰",
                fontSize = 80.sp
            )

            Text(
                text = taskTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 关联应用卡片（如果有）
            if (targetPkgName != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    onClick = onOpenApp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 应用图标
                        if (targetAppIcon != null) {
                            Image(
                                bitmap = targetAppIcon.toBitmap(96, 96).asImageBitmap(),
                                contentDescription = targetAppName,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE53935).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📱",
                                    fontSize = 28.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "点击打开",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = targetAppName ?: targetPkgName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                        }

                        Text(
                            text = "→",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 关闭按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "关闭闹钟",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

