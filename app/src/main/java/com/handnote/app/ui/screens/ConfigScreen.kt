package com.handnote.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.handnote.app.data.entity.Anniversary
import com.handnote.app.data.entity.ShiftRule
import com.handnote.app.ui.components.AppPickerButton
import com.handnote.app.ui.viewmodel.MainViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConfigScreen(
    viewModel: MainViewModel
) {
    val shiftRules by viewModel.allShiftRules.collectAsState()
    val anniversaries by viewModel.allAnniversaries.collectAsState()

    var editingShiftRule by remember { mutableStateOf<ShiftRule?>(null) }
    var showShiftRuleDialog by remember { mutableStateOf(false) }

    var editingAnniversary by remember { mutableStateOf<Anniversary?>(null) }
    var showAnniversaryDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "排班规则",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "当前规则：${shiftRules.size} 条",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        editingShiftRule = null
                        showShiftRuleDialog = true
                    }) {
                        Text("新增规则")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (shiftRules.isEmpty()) {
                    Text(
                        text = "暂无规则，请点击「新增规则」创建。",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 外层已是可滚动容器；这里必须先约束高度，再允许内部滚动，
                            // 否则会在外层 scroll 传递的无限高度约束下崩溃。
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        shiftRules.forEach { rule ->
                            ShiftRuleItem(
                                shiftRule = rule,
                                onEdit = {
                                    editingShiftRule = it
                                    showShiftRuleDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteShiftRule(it)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "纪念日 / 特殊事件",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "当前纪念日：${anniversaries.size} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        editingAnniversary = null
                        showAnniversaryDialog = true
                    }) {
                        Text("新增纪念日")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (anniversaries.isEmpty()) {
                    Text(
                        text = "暂无纪念日配置，请点击「新增纪念日」创建。",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 同上：先约束高度，再允许内部滚动，避免无限高度约束崩溃
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        anniversaries.forEach { anniversary ->
                            AnniversaryItem(
                                anniversary = anniversary,
                                onEdit = {
                                    editingAnniversary = it
                                    showAnniversaryDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteAnniversary(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShiftRuleDialog) {
        ShiftRuleEditDialog(
            initial = editingShiftRule,
            onDismiss = { showShiftRuleDialog = false },
            onConfirm = { rule ->
                viewModel.upsertShiftRule(rule)
                showShiftRuleDialog = false
            }
        )
    }

    if (showAnniversaryDialog) {
        AnniversaryEditDialog(
            initial = editingAnniversary,
            onDismiss = { showAnniversaryDialog = false },
            onConfirm = { anniversary ->
                viewModel.upsertAnniversary(anniversary)
                showAnniversaryDialog = false
            }
        )
    }
}

@Composable
private fun ShiftRuleItem(
    shiftRule: ShiftRule,
    onEdit: (ShiftRule) -> Unit,
    onDelete: (ShiftRule) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shiftRule.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                TextButton(onClick = { onEdit(shiftRule) }) {
                    Text("编辑")
                }
                TextButton(onClick = { onDelete(shiftRule) }) {
                    Text("删除")
                }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val startDate = LocalDate.ofEpochDay(shiftRule.startDate / 86400000L)
            val reminderLevelText = when (shiftRule.defaultReminderLevel) {
                0 -> "无提醒"
                1 -> "弱提醒"
                2 -> "强提醒"
                else -> "未知"
            }
            
            Text(
                text = "起始日期：${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "周期长度：${shiftRule.cycleDays} 天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "默认提醒等级：$reminderLevelText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "跳过节假日：${if (shiftRule.skipHoliday) "是" else "否"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 显示配置摘要
            if (shiftRule.shiftConfig.isNotBlank() && shiftRule.shiftConfig != "[]") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已配置排班时间点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnniversaryItem(
    anniversary: Anniversary,
    onEdit: (Anniversary) -> Unit,
    onDelete: (Anniversary) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anniversary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                TextButton(onClick = { onEdit(anniversary) }) {
                    Text("编辑")
                }
                TextButton(onClick = { onDelete(anniversary) }) {
                    Text("删除")
                }
            }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val reminderLevelText = when (anniversary.reminderLevel) {
                0 -> "无提醒"
                1 -> "弱提醒"
                2 -> "强提醒"
                else -> "未知"
            }
            
            Text(
                text = "日期：${anniversary.targetDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "提醒等级：$reminderLevelText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftRuleEditDialog(
    initial: ShiftRule?,
    onDismiss: () -> Unit,
    onConfirm: (ShiftRule) -> Unit
) {
    val now = remember { LocalDate.now() }
    val defaultStartDate = remember {
        initial?.let { LocalDate.ofEpochDay(it.startDate / 86400000L) } ?: now
    }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var startDateText by remember {
        mutableStateOf(defaultStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var cycleDaysText by remember { mutableStateOf(initial?.cycleDays?.toString() ?: "3") }
    var skipHoliday by remember { mutableStateOf(initial?.skipHoliday ?: true) }
    var defaultReminderLevelText by remember {
        mutableStateOf(initial?.defaultReminderLevel?.toString() ?: "2")
    }

    // 简化模式的状态
    var useAdvancedMode by remember { mutableStateOf(false) }
    var simpleTime by remember { mutableStateOf("08:00") }
    var simpleTargetPkg by remember { mutableStateOf<String?>(null) }

    // 从现有配置解析简化模式的值
    LaunchedEffect(initial) {
        initial?.shiftConfig?.let { config ->
            try {
                val arr = JSONArray(config)
                if (arr.length() > 0) {
                    val firstDay = arr.getJSONObject(0)
                    val timeSlots = firstDay.optJSONArray("timeSlots")
                    if (timeSlots != null && timeSlots.length() > 0) {
                        val firstSlot = timeSlots.getJSONObject(0)
                        simpleTime = firstSlot.optString("time", "08:00")
                        val pkg = firstSlot.optString("targetPkgName", "")
                        simpleTargetPkg = if (pkg.isNotBlank()) pkg else null
                    }
                }
            } catch (e: Exception) {
                // 解析失败，使用默认值
            }
        }
    }

    var shiftConfigJson by remember {
        mutableStateOf(
            initial?.shiftConfig ?: """[
  {
    "dayIndex": 0,
    "reminderLevel": 2,
    "timeSlots": [
      {"time": "08:00", "targetPkgName": ""}
    ]
  }
]"""
        )
    }
    var showConfigHelp by remember { mutableStateOf(false) }
    var configError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // 验证 JSON 配置（仅高级模式）
    LaunchedEffect(shiftConfigJson, useAdvancedMode) {
        if (useAdvancedMode) {
            configError = try {
                if (shiftConfigJson.isBlank()) {
                    null
                } else {
                    JSONArray(shiftConfigJson)
                    null
                }
            } catch (e: Exception) {
                "JSON 格式错误: ${e.message}"
            }
        } else {
            configError = null
        }
    }

    // 简化模式生成 JSON
    fun generateSimpleConfig(): String {
        val cycleDays = cycleDaysText.toIntOrNull() ?: 1
        val reminderLevel = defaultReminderLevelText.toIntOrNull() ?: 2
        val arr = JSONArray()
        for (i in 0 until cycleDays) {
            val dayObj = JSONObject()
            dayObj.put("dayIndex", i)
            dayObj.put("reminderLevel", reminderLevel)
            val slots = JSONArray()
            val slot = JSONObject()
            slot.put("time", simpleTime)
            slot.put("targetPkgName", simpleTargetPkg ?: "")
            slots.put(slot)
            dayObj.put("timeSlots", slots)
            arr.put(dayObj)
        }
        return arr.toString(2)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initial == null) "新增排班规则" else "编辑排班规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("规则名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：早班打卡") }
                )

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        if (it.matches(Regex("\\d{0,4}-?\\d{0,2}-?\\d{0,2}")) || it.isEmpty()) {
                            startDateText = it
                        }
                    },
                    label = { Text("起始日期（YYYY-MM-DD）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("周期将从该日期开始计算") }
                )

                OutlinedTextField(
                    value = cycleDaysText,
                    onValueChange = { cycleDaysText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("周期天数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("排班周期长度，例如：3天一轮") }
                )

                // 提醒等级选择
                Text(
                    text = "提醒等级",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = defaultReminderLevelText == "0",
                        onClick = { defaultReminderLevelText = "0" },
                        label = { Text("无提醒") }
                    )
                    FilterChip(
                        selected = defaultReminderLevelText == "1",
                        onClick = { defaultReminderLevelText = "1" },
                        label = { Text("弱提醒") }
                    )
                    FilterChip(
                        selected = defaultReminderLevelText == "2",
                        onClick = { defaultReminderLevelText = "2" },
                        label = { Text("强提醒") }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "跳过节假日", modifier = Modifier.weight(1f))
                    Switch(
                        checked = skipHoliday,
                        onCheckedChange = { skipHoliday = it }
                    )
                }

                Divider()

                // 模式切换
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (useAdvancedMode) "高级模式" else "简化模式",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { useAdvancedMode = !useAdvancedMode }) {
                        Text(if (useAdvancedMode) "切换到简化" else "切换到高级")
                    }
                }

                if (!useAdvancedMode) {
                    // 简化模式
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "每天在同一时间提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = simpleTime,
                                onValueChange = {
                                    if (it.matches(Regex("\\d{0,2}:?\\d{0,2}")) || it.isEmpty()) {
                                        simpleTime = it
                                    }
                                },
                                label = { Text("提醒时间") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("08:00") }
                            )

                            Text(
                                text = "关联应用（可选）",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "闹钟响起时，可以快速打开选中的应用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AppPickerButton(
                                currentPackageName = simpleTargetPkg,
                                onPackageSelected = { simpleTargetPkg = it }
                            )
                        }
                    }
                } else {
                    // 高级模式
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "排班配置 JSON",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showConfigHelp = !showConfigHelp }) {
                            Icon(Icons.Default.Info, contentDescription = "配置说明")
                        }
                    }

                    if (showConfigHelp) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "配置格式说明：",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• dayIndex: 周期中的第几天（0 开始）\n" +
                                            "• reminderLevel: 提醒等级（0/1/2）\n" +
                                            "• timeSlots: 时间点数组\n" +
                                            "  - time: 时间（HH:mm）\n" +
                                            "  - targetPkgName: 目标应用包名",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = shiftConfigJson,
                        onValueChange = { shiftConfigJson = it },
                        label = { Text("排班配置 JSON") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        isError = configError != null,
                        supportingText = {
                            configError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("配置每个周期日的提醒时间和等级")
                        },
                        singleLine = false
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cycleDays = cycleDaysText.toIntOrNull() ?: 1
                    val reminderLevel = defaultReminderLevelText.toIntOrNull() ?: 2
                    val startDate = try {
                        LocalDate.parse(startDateText, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        defaultStartDate
                    }
                    val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

                    val finalConfig = if (useAdvancedMode) {
                        shiftConfigJson.ifBlank { "[]" }
                    } else {
                        generateSimpleConfig()
                    }

                    val rule = ShiftRule(
                        id = initial?.id ?: 0,
                        title = title.ifBlank { "未命名规则" },
                        startDate = startMillis,
                        cycleDays = cycleDays,
                        shiftConfig = finalConfig,
                        skipHoliday = skipHoliday,
                        defaultReminderLevel = reminderLevel
                    )
                    onConfirm(rule)
                },
                enabled = (useAdvancedMode && configError == null || !useAdvancedMode) && title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AnniversaryEditDialog(
    initial: Anniversary?,
    onDismiss: () -> Unit,
    onConfirm: (Anniversary) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var dateText by remember { mutableStateOf(initial?.targetDate ?: "") }
    var levelText by remember { mutableStateOf(initial?.reminderLevel?.toString() ?: "1") }
    var dateError by remember { mutableStateOf<String?>(null) }
    
    // 验证日期格式
    LaunchedEffect(dateText) {
        dateError = if (dateText.isBlank()) {
            null
        } else {
            try {
                LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE)
                null
            } catch (e: Exception) {
                "日期格式错误，请使用 YYYY-MM-DD 格式"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (initial == null) "新增纪念日" else "编辑纪念日") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：生日、结婚纪念日") }
                )
                
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { 
                        // 简单验证日期格式
                        if (it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || it.isEmpty()) {
                            dateText = it
                        }
                    },
                    label = { Text("日期（YYYY-MM-DD）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = dateError != null,
                    supportingText = {
                        dateError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        } ?: Text("每年该日期都会提醒（仅匹配月日，忽略年份）")
                    },
                    placeholder = { Text("例如：2024-01-01") }
                )
                
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { levelText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("提醒等级（0/1/2）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("0=无提醒, 1=弱提醒, 2=强提醒")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val level = levelText.toIntOrNull() ?: 1
                    val finalDate = if (dateText.isBlank()) {
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else {
                        try {
                            LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: Exception) {
                            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                    }
                    val anniversary = Anniversary(
                        id = initial?.id ?: 0,
                        title = title.ifBlank { "未命名纪念日" },
                        targetDate = finalDate,
                        reminderLevel = level,
                        reminderTime = initial?.reminderTime
                    )
                    onConfirm(anniversary)
                },
                enabled = dateError == null && title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

