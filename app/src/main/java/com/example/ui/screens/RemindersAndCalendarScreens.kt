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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarSystem
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderStatus
import com.example.data.local.YadetSettings
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtils

private enum class TaskFilter(val label: String) {
    ALL("همه"),
    TODAY("امروز"),
    TOMORROW("فردا"),
    WEEK("هفته"),
    MONTH("ماه"),
    OVERDUE("عقب‌افتاده"),
    COMPLETED("انجام‌شده")
}

private enum class TaskSort(val label: String) {
    TIME("زمان"),
    PRIORITY("اولویت"),
    TITLE("عنوان")
}

@Composable
fun AllTasksScreen(
    reminders: List<ReminderEntity>,
    settings: YadetSettings,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var selectedSort by remember { mutableStateOf(TaskSort.TIME) }

    val now = System.currentTimeMillis()
    val startToday = PersianCalendarUtils.startOfDayMillis(now)
    val endToday = PersianCalendarUtils.endOfDayMillis(now)
    val startTomorrow = startToday + 24L * 3600 * 1000
    val endTomorrow = endToday + 24L * 3600 * 1000
    val endWeek = endToday + 6L * 24 * 3600 * 1000
    val endMonth = endToday + 29L * 24 * 3600 * 1000

    val filtered = remember(reminders, searchQuery, selectedFilter, selectedSort) {
        reminders
            .filter { r ->
                val matchesQuery = searchQuery.isBlank() ||
                    r.title.contains(searchQuery, ignoreCase = true) ||
                    r.description.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.TODAY -> r.triggerAtMillis in startToday..endToday
                    TaskFilter.TOMORROW -> r.triggerAtMillis in startTomorrow..endTomorrow
                    TaskFilter.WEEK -> r.triggerAtMillis in startToday..endWeek
                    TaskFilter.MONTH -> r.triggerAtMillis in startToday..endMonth
                    TaskFilter.OVERDUE -> r.status == ReminderStatus.ACTIVE.name && r.triggerAtMillis < now
                    TaskFilter.COMPLETED -> r.status == ReminderStatus.COMPLETED.name
                }
                matchesQuery && matchesFilter
            }
            .let { list ->
                when (selectedSort) {
                    TaskSort.TIME -> list.sortedBy { it.triggerAtMillis }
                    TaskSort.PRIORITY -> list.sortedByDescending { it.priority }
                    TaskSort.TITLE -> list.sortedBy { it.title }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجو در کارها و یادآورها...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TaskFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مرتب‌سازی:", style = MaterialTheme.typography.labelMedium)
            TaskSort.entries.forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { selectedSort = sort },
                    label = { Text(sort.label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "موردی یافت نشد.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(filtered, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        isJalali = settings.calendarSystem == CalendarSystem.JALALI,
                        is24Hour = settings.is24Hour,
                        onClick = { onEditReminder(reminder) },
                        onToggleComplete = { onToggleComplete(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    reminders: List<ReminderEntity>,
    settings: YadetSettings,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit
) {
    val todayJalali = remember { PersianCalendarUtils.millisToJalali(System.currentTimeMillis()) }
    var displayYear by remember { mutableIntStateOf(todayJalali.year) }
    var displayMonth by remember { mutableIntStateOf(todayJalali.month) }
    var selectedDate by remember { mutableStateOf(todayJalali) }

    val monthLength = remember(displayYear, displayMonth) {
        PersianCalendarUtils.jalaliMonthLength(displayYear, displayMonth)
    }
    val firstDayOfWeek = remember(displayYear, displayMonth) {
        val m = PersianCalendarUtils.jalaliToMillis(JalaliDate(displayYear, displayMonth, 1), 12, 0)
        PersianCalendarUtils.getPersianDayOfWeekIndex(m)
    }

    val daysWithReminders = remember(reminders, displayYear, displayMonth) {
        reminders.mapNotNull { r ->
            val j = PersianCalendarUtils.millisToJalali(r.triggerAtMillis)
            if (j.year == displayYear && j.month == displayMonth) j.day else null
        }.toSet()
    }

    val selectedDayReminders = remember(reminders, selectedDate) {
        reminders.filter { r ->
            PersianCalendarUtils.millisToJalali(r.triggerAtMillis) == selectedDate
        }.sortedBy { it.triggerAtMillis }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (displayMonth == 1) {
                                displayMonth = 12
                                displayYear -= 1
                            } else displayMonth -= 1
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                        }

                        Text(
                            text = "${PersianCalendarUtils.jalaliMonthName(displayMonth)} ${PersianCalendarUtils.toPersianDigits(displayYear)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                displayYear = todayJalali.year
                                displayMonth = todayJalali.month
                                selectedDate = todayJalali
                            }) {
                                Text("امروز")
                            }
                            IconButton(onClick = {
                                if (displayMonth == 12) {
                                    displayMonth = 1
                                    displayYear += 1
                                } else displayMonth += 1
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    val headers = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        headers.forEach { h ->
                            Text(
                                text = h,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    val totalCells = firstDayOfWeek + monthLength
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val dayNum = (r * 7 + c) - firstDayOfWeek + 1
                                    if (dayNum in 1..monthLength) {
                                        val cellDate = JalaliDate(displayYear, displayMonth, dayNum)
                                        val isSelected = cellDate == selectedDate
                                        val isToday = cellDate == todayJalali
                                        val hasEvents = dayNum in daysWithReminders
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.secondary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { selectedDate = cellDate },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = PersianCalendarUtils.toPersianDigits(dayNum),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onSecondary
                                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasEvents) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onSecondary
                                                            else MaterialTheme.colorScheme.secondary
                                                        )
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f).height(44.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "برنامه‌های ${selectedDate.formatFull()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedDayReminders.isEmpty()) {
            item {
                Text(
                    "در این روز یادآوری ثبت نشده است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(selectedDayReminders, key = { it.id }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    isJalali = settings.calendarSystem == CalendarSystem.JALALI,
                    is24Hour = settings.is24Hour,
                    onClick = { onEditReminder(reminder) },
                    onToggleComplete = { onToggleComplete(reminder) }
                )
            }
        }
    }
}
