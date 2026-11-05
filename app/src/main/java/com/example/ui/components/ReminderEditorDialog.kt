package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorDialog(
    existing: ReminderEntity? = null,
    categories: List<CategoryEntity>,
    projects: List<ProjectEntity>,
    isJalali: Boolean,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit,
    onDelete: ((ReminderEntity) -> Unit)? = null
) {
    val defaultTrigger = remember(existing) {
        existing?.triggerAtMillis ?: (System.currentTimeMillis() + 3600_000L)
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var triggerMillis by remember { mutableLongStateOf(defaultTrigger) }
    var priority by remember { mutableIntStateOf(existing?.priority ?: ReminderPriority.NORMAL.level) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var projectId by remember { mutableStateOf(existing?.projectId) }
    var recurrenceType by remember {
        mutableStateOf(
            runCatching { RecurrenceType.valueOf(existing?.recurrenceType ?: "NONE") }
                .getOrDefault(RecurrenceType.NONE)
        )
    }
    var recurrenceInterval by remember { mutableIntStateOf(existing?.recurrenceInterval ?: 1) }
    var recurrenceDaysMask by remember { mutableIntStateOf(existing?.recurrenceDaysMask ?: 0) }
    var recurrenceEndMillis by remember { mutableStateOf(existing?.recurrenceEndMillis) }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var url by remember { mutableStateOf(existing?.url ?: "") }
    var titleError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showProjectMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (existing == null) "یادآور جدید" else "ویرایش یادآور",
                    style = MaterialTheme.typography.titleMedium
                )
                if (existing != null && onDelete != null) {
                    IconButton(onClick = { onDelete(existing) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف یادآور",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("عنوان یادآور *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("لطفاً عنوان یادآور را وارد کنید") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Visual Date & Time buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_date_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "انتخاب تاریخ")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            PersianCalendarUtils.millisToJalali(triggerMillis).formatShort(),
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_time_button")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "انتخاب ساعت")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            PersianCalendarUtils.formatTimeOnly(triggerMillis, is24Hour),
                            maxLines = 1
                        )
                    }
                }

                // Visual Recurrence button
                OutlinedButton(
                    onClick = { showRecurrencePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = "تکرار")
                    Spacer(Modifier.width(8.dp))
                    Text("تکرار: ${recurrenceType.persianTitle}")
                }

                // Priority chips
                Text("اولویت:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p.level,
                            onClick = { priority = p.level },
                            label = { Text(p.persianLabel) }
                        )
                    }
                }

                // Category & Project dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val catName = categories.find { it.id == categoryId }?.name ?: "دسته‌بندی"
                        OutlinedButton(
                            onClick = { showCategoryMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(catName, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون دسته‌بندی") },
                                onClick = {
                                    categoryId = null
                                    showCategoryMenu = false
                                }
                            )
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        categoryId = c.id
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val projName = projects.find { it.id == projectId }?.name ?: "پروژه"
                        OutlinedButton(
                            onClick = { showProjectMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(projName, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون پروژه") },
                                onClick = {
                                    projectId = null
                                    showProjectMenu = false
                                }
                            )
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        projectId = p.id
                                        showProjectMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مکان (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("پیوند / لینک (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@TextButton
                    }
                    val entity = (existing ?: ReminderEntity(title = title.trim(), triggerAtMillis = triggerMillis)).copy(
                        title = title.trim(),
                        description = description.trim(),
                        triggerAtMillis = triggerMillis,
                        priority = priority,
                        categoryId = categoryId,
                        projectId = projectId,
                        recurrenceType = recurrenceType.name,
                        recurrenceInterval = recurrenceInterval,
                        recurrenceDaysMask = recurrenceDaysMask,
                        recurrenceStartMillis = if (recurrenceType != RecurrenceType.NONE) triggerMillis else null,
                        recurrenceEndMillis = recurrenceEndMillis,
                        location = location.trim(),
                        url = url.trim()
                    )
                    onSave(entity)
                },
                modifier = Modifier.testTag("save_reminder_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    if (showDatePicker) {
        VisualPersianDatePickerDialog(
            initialMillis = triggerMillis,
            isJalali = isJalali,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newMillis ->
                triggerMillis = newMillis
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        VisualTimePickerDialog(
            initialMillis = triggerMillis,
            is24Hour = is24Hour,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { h, m ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = triggerMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                }
                triggerMillis = cal.timeInMillis
                showTimePicker = false
            }
        )
    }

    if (showRecurrencePicker) {
        VisualRecurrencePickerDialog(
            initialType = recurrenceType,
            initialInterval = recurrenceInterval,
            initialDaysMask = recurrenceDaysMask,
            initialEndMillis = recurrenceEndMillis,
            isJalali = isJalali,
            onDismiss = { showRecurrencePicker = false },
            onConfirm = { type, interval, mask, end ->
                recurrenceType = type
                recurrenceInterval = interval
                recurrenceDaysMask = mask
                recurrenceEndMillis = end
                showRecurrencePicker = false
            }
        )
    }
}
