package com.handnote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

/**
 * 日历日期数据类
 */
data class CalendarDay(
    val date: LocalDate?,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val reminderLevel: Int = 0 // 0: 无提醒, 1: 弱提醒, 2: 强提醒
)

/**
 * 日历视图组件
 */
@Composable
fun CalendarView(
    selectedDate: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit = {},
    taskRecordsByDate: Map<String, Int> = emptyMap(), // 日期 -> 最高提醒等级
    modifier: Modifier = Modifier
) {
    val yearMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = yearMonth.lengthOfMonth()
    
    // 计算需要显示的日期列表
    val calendarDays = mutableListOf<CalendarDay>()
    
    // 添加星期标题行
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    
    // 添加第一周的空白日期（上个月的日期）
    val daysToAddBefore = (firstDayOfWeek.value % 7)
    for (i in daysToAddBefore - 1 downTo 0) {
        calendarDays.add(CalendarDay(null, 0, false))
    }
    
    // 添加当前月的日期
    for (day in 1..daysInMonth) {
        val date = yearMonth.atDay(day)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val reminderLevel = taskRecordsByDate[dateStr] ?: 0
        calendarDays.add(
            CalendarDay(
                date = date,
                dayOfMonth = day,
                isCurrentMonth = true,
                reminderLevel = reminderLevel
            )
        )
    }
    
    // 填充最后一周的空白日期（下个月的日期）
    val totalCells = calendarDays.size
    val remainingCells = (7 - (totalCells % 7)) % 7
    for (i in 0 until remainingCells) {
        calendarDays.add(CalendarDay(null, 0, false))
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // 月份标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${yearMonth.year}年${yearMonth.monthValue}月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 星期标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 日期网格（使用非滚动的 Column + Row 布局，避免嵌套滚动问题）
        // calendarDays.size 已经是7的倍数（因为填充了空白），所以行数就是 size / 7
        val rows = calendarDays.size / 7
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 将日期列表按行分组（weekRow 避免与上方 weekDays 星期标题变量冲突）
            calendarDays.chunked(7).forEach { weekRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekRow.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            CalendarDayItem(
                                day = day,
                                isSelected = day.date == selectedDate,
                                onClick = { day.date?.let { onDateSelected(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个日期单元格
 */
@Composable
fun CalendarDayItem(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(enabled = day.date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (day.date != null) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (day.isCurrentMonth) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // 根据提醒等级显示不同的标记
                Spacer(modifier = Modifier.height(2.dp))
                when (day.reminderLevel) {
                    2 -> {
                        // 强提醒：实心红点
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                    }
                    1 -> {
                        // 弱提醒：空心蓝点
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(1.dp, Color(0xFF2196F3), CircleShape)
                        )
                    }
                }
            }
        }
    }
}


