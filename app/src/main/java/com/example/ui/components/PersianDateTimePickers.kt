package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import java.util.Calendar

@Composable
fun VisualPersianDatePickerDialog(
    initialMillis: Long,
    isJalali: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val todayJalali = remember { PersianCalendarUtils.millisToJalali(System.currentTimeMillis()) }
    val initialJalali = remember(initialMillis) { PersianCalendarUtils.millisToJalali(initialMillis) }

    var displayYear by remember { mutableIntStateOf(initialJalali.year) }
    var displayMonth by remember { mutableIntStateOf(initialJalali.month) }
    var selectedDate by remember { mutableStateOf(initialJalali) }

    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    val monthLength = remember(displayYear, displayMonth) {
        PersianCalendarUtils.jalaliMonthLength(displayYear, displayMonth)
    }
    val firstDayOfWeek = remember(displayYear, displayMonth) {
        val m = PersianCalendarUtils.jalaliToMillis(JalaliDate(displayYear, displayMonth, 1), 12, 0)
        PersianCalendarUtils.getPersianDayOfWeekIndex(m)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isJalali) "انتخاب تاریخ شمسی" else "انتخاب تاریخ",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        displayYear = todayJalali.year
                        displayMonth = todayJalali.month
                        selectedDate = todayJalali
                    }
                ) {
                    Icon(Icons.Default.Today, contentDescription = "امروز", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("برو به امروز")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Month & Year selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (displayMonth == 1) {
                            displayMonth = 12
                            displayYear -= 1
                        } else {
                            displayMonth -= 1
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            OutlinedButton(onClick = { showMonthDropdown = true }) {
                                Text(PersianCalendarUtils.jalaliMonthName(displayMonth))
                            }
                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false }
                            ) {
                                (1..12).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(PersianCalendarUtils.jalaliMonthName(m)) },
                                        onClick = {
                                            displayMonth = m
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            OutlinedButton(onClick = { showYearDropdown = true }) {
                                Text(PersianCalendarUtils.toPersianDigits(displayYear))
                            }
                            DropdownMenu(
                                expanded = showYearDropdown,
                                onDismissRequest = { showYearDropdown = false }
                            ) {
                                ((todayJalali.year - 2)..(todayJalali.year + 10)).forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(PersianCalendarUtils.toPersianDigits(y)) },
                                        onClick = {
                                            displayYear = y
                                            showYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = {
                        if (displayMonth == 12) {
                            displayMonth = 1
                            displayYear += 1
                        } else {
                            displayMonth += 1
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Weekday headers
                val headers = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                Row(modifier = Modifier.fillMaxWidth()) {
                    headers.forEach { h ->
                        Text(
                            text = h,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Calendar grid
                val totalCells = firstDayOfWeek + monthLength
                val rows = (totalCells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - firstDayOfWeek + 1
                                if (dayNum in 1..monthLength) {
                                    val cellDate = JalaliDate(displayYear, displayMonth, dayNum)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == todayJalali
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.secondary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedDate = cellDate },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = PersianCalendarUtils.toPersianDigits(dayNum),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onSecondary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).height(40.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تاریخ انتخاب‌شده: ${selectedDate.formatFull()}",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
                val h = cal.get(Calendar.HOUR_OF_DAY)
                val m = cal.get(Calendar.MINUTE)
                onDateSelected(PersianCalendarUtils.jalaliToMillis(selectedDate, h, m))
            }) {
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualTimePickerDialog(
    initialMillis: Long,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (hour24: Int, minute: Int) -> Unit
) {
    val cal = remember(initialMillis) { Calendar.getInstance().apply { timeInMillis = initialMillis } }
    var hour24 by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }

    val quickMinutes = listOf(0, 5, 10, 15, 20, 30, 45, 50)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب ساعت", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Digital display
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayHour = if (is24Hour) {
                        hour24
                    } else {
                        when {
                            hour24 == 0 -> 12
                            hour24 > 12 -> hour24 - 12
                            else -> hour24
                        }
                    }
                    val periodText = if (is24Hour) "" else if (hour24 < 12) " صبح" else " عصر"
                    Text(
                        text = PersianCalendarUtils.toPersianDigits(
                            "${displayHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                        ) + periodText,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }

                if (!is24Hour) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = hour24 < 12,
                            onClick = { if (hour24 >= 12) hour24 -= 12 },
                            label = { Text("صبح (ق.ظ)") }
                        )
                        FilterChip(
                            selected = hour24 >= 12,
                            onClick = { if (hour24 < 12) hour24 += 12 },
                            label = { Text("عصر (ب.ظ)") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Hour & Minute steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ساعت", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour24 = (hour24 + 23) % 24 }) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش ساعت")
                            }
                            Text(
                                PersianCalendarUtils.toPersianDigits(hour24.toString().padStart(2, '0')),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { hour24 = (hour24 + 1) % 24 }) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش ساعت")
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("دقیقه", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = (minute + 59) % 60 }) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش دقیقه")
                            }
                            Text(
                                PersianCalendarUtils.toPersianDigits(minute.toString().padStart(2, '0')),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { minute = (minute + 1) % 60 }) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش دقیقه")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("انتخاب سریع دقیقه:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickMinutes.forEach { m ->
                        FilterChip(
                            selected = minute == m,
                            onClick = { minute = m },
                            label = { Text(PersianCalendarUtils.toPersianDigits(m.toString().padStart(2, '0'))) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(hour24, minute) }) {
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualRecurrencePickerDialog(
    initialType: RecurrenceType,
    initialInterval: Int,
    initialDaysMask: Int,
    initialEndMillis: Long?,
    isJalali: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceType, Int, Int, Long?) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var interval by remember { mutableIntStateOf(initialInterval.coerceAtLeast(1)) }
    var daysMask by remember { mutableIntStateOf(initialDaysMask) }
    var endMillis by remember { mutableStateOf(initialEndMillis) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val weekDays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم تکرار یادآور", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(RecurrenceType.entries) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(type.persianTitle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (selectedType == RecurrenceType.EVERY_N_DAYS || selectedType == RecurrenceType.EVERY_N_HOURS) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedType == RecurrenceType.EVERY_N_DAYS) "فاصله (روز):" else "فاصله (ساعت):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (interval > 1) interval-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش")
                            }
                            Text(
                                PersianCalendarUtils.toPersianDigits(interval),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { interval++ }) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش")
                            }
                        }
                    }
                }

                if (selectedType == RecurrenceType.SELECTED_WEEKDAYS || selectedType == RecurrenceType.ALL_EXCEPT_SELECTED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selectedType == RecurrenceType.SELECTED_WEEKDAYS) "روزهای هفته:" else "به‌جز روزهای:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEachIndexed { idx, label ->
                            val checked = (daysMask and (1 shl idx)) != 0
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    daysMask = daysMask xor (1 shl idx)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (selectedType != RecurrenceType.NONE) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = endMillis?.let {
                                "پایان: ${PersianCalendarUtils.millisToJalali(it).formatShort()}"
                            } ?: "بدون تاریخ پایان",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            TextButton(onClick = { showEndDatePicker = true }) {
                                Text("تعیین پایان")
                            }
                            if (endMillis != null) {
                                TextButton(onClick = { endMillis = null }) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedType, interval, daysMask, endMillis) }) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    if (showEndDatePicker) {
        VisualPersianDatePickerDialog(
            initialMillis = endMillis ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
            isJalali = isJalali,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = {
                endMillis = PersianCalendarUtils.endOfDayMillis(it)
                showEndDatePicker = false
            }
        )
    }
}
