package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarSystem
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.ReminderStatus
import com.example.data.local.YadetSettings
import com.example.ui.StatsRange
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.LightTurquoise
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Turquoise
import com.example.util.PersianCalendarUtils

@Composable
fun DashboardScreen(
    reminders: List<ReminderEntity>,
    settings: YadetSettings,
    statsRange: StatsRange,
    onStatsRangeChange: (StatsRange) -> Unit,
    onCreateReminderClick: () -> Unit,
    onEditReminderClick: (ReminderEntity) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onOpenAiChat: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isJalali = settings.calendarSystem == CalendarSystem.JALALI
    val todayJalali = remember(now) { PersianCalendarUtils.millisToJalali(now) }
    val weekday = remember(now) { PersianCalendarUtils.getPersianDayOfWeekName(now) }

    val rangeStart = remember(now, statsRange) {
        when (statsRange) {
            StatsRange.TODAY -> PersianCalendarUtils.startOfDayMillis(now)
            StatsRange.WEEK -> PersianCalendarUtils.startOfDayMillis(now) - 6L * 24 * 3600 * 1000
            StatsRange.MONTH -> PersianCalendarUtils.startOfDayMillis(now) - 29L * 24 * 3600 * 1000
        }
    }
    val rangeEnd = remember(now) { PersianCalendarUtils.endOfDayMillis(now) }

    val inRange = remember(reminders, rangeStart, rangeEnd) {
        reminders.filter { it.triggerAtMillis in rangeStart..rangeEnd }
    }
    val todayCount = inRange.count { it.status == ReminderStatus.ACTIVE.name }
    val completedCount = inRange.count { it.status == ReminderStatus.COMPLETED.name }
    val overdueCount = reminders.count {
        it.status == ReminderStatus.ACTIVE.name && it.triggerAtMillis < now
    }

    val priorityTasks = remember(reminders) {
        reminders.filter {
            it.status == ReminderStatus.ACTIVE.name && it.priority >= ReminderPriority.HIGH.level
        }.take(4)
    }

    val upcomingTasks = remember(reminders) {
        reminders.filter {
            it.status == ReminderStatus.ACTIVE.name && it.triggerAtMillis >= now
        }.sortedBy { it.triggerAtMillis }.take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepNavy, DarkBlue, Turquoise.copy(alpha = 0.85f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$weekday، ${todayJalali.formatFull()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "خلاصه وضعیت ${statsRange.persianLabel} شما",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onCreateReminderClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Turquoise,
                                contentColor = DeepNavy
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("dashboard_add_reminder_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "ایجاد یادآور")
                            Spacer(Modifier.width(4.dp))
                            Text("یادآور جدید", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Range Selector
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsRange.entries.forEach { range ->
                            FilterChip(
                                selected = statsRange == range,
                                onClick = { onStatsRangeChange(range) },
                                label = { Text(range.persianLabel) }
                            )
                        }
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "در پیش",
                    count = todayCount,
                    icon = Icons.Default.Schedule,
                    accent = Turquoise,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "انجام‌شده",
                    count = completedCount,
                    icon = Icons.Default.CheckCircle,
                    accent = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "عقب‌افتاده",
                    count = overdueCount,
                    icon = Icons.Default.Warning,
                    accent = ErrorCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick AI Assistant & Voice Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAiChat() }
                    .testTag("dashboard_ai_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "دستیار هوش مصنوعی",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "گفتگو با دستیار هوشمند یادت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "مشورت، برنامه‌ریزی و مدیریت یادآورها با متن یا صدا",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onOpenAiChat) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "مکالمه صوتی",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Priority Tasks
        if (priorityTasks.isNotEmpty()) {
            item {
                Text(
                    "کارهای مهم و اولویت‌دار",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(priorityTasks, key = { "prio_${it.id}" }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    isJalali = isJalali,
                    is24Hour = settings.is24Hour,
                    onClick = { onEditReminderClick(reminder) },
                    onToggleComplete = { onToggleComplete(reminder) }
                )
            }
        }

        // Upcoming Schedule
        item {
            Text(
                "برنامه‌های نزدیک",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (upcomingTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "هیچ برنامه نزدیکی ثبت نشده است",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "می‌توانید با دکمه «یادآور جدید» یا گفتگو با دستیار، برنامه خود را ثبت کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(upcomingTasks, key = { "up_${it.id}" }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    isJalali = isJalali,
                    is24Hour = settings.is24Hour,
                    onClick = { onEditReminderClick(reminder) },
                    onToggleComplete = { onToggleComplete(reminder) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Text(
                text = PersianCalendarUtils.toPersianDigits(count),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    isJalali: Boolean,
    is24Hour: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val isCompleted = reminder.status == ReminderStatus.COMPLETED.name
    val isOverdue = !isCompleted && reminder.triggerAtMillis < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "انجام‌شده" else "تکمیل کار",
                    tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = PersianCalendarUtils.formatDateTime(reminder.triggerAtMillis, isJalali, is24Hour),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOverdue) ErrorCoral else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
