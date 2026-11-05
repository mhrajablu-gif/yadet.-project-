package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarSystem
import com.example.data.local.CategoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderStatus
import com.example.data.local.YadetSettings
import com.example.util.PersianCalendarUtils

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    reminders: List<ReminderEntity>,
    onSaveProject: (ProjectEntity) -> Unit,
    onTrashProject: (ProjectEntity) -> Unit
) {
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("پروژه‌ها", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("پروژه جدید")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(projects, key = { it.id }) { proj ->
                val projReminders = reminders.filter { it.projectId == proj.id }
                val total = projReminders.size
                val done = projReminders.count { it.status == ReminderStatus.COMPLETED.name }
                val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingProject = proj },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onTrashProject(proj) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف پروژه")
                            }
                        }
                        if (proj.description.isNotBlank()) {
                            Text(proj.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "پیشرفت: ${PersianCalendarUtils.toPersianDigits((progress * 100).toInt())}٪ (${PersianCalendarUtils.toPersianDigits(total - done)} کار باقیمانده)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingProject != null) {
        var name by remember(editingProject) { mutableStateOf(editingProject?.name ?: "") }
        var desc by remember(editingProject) { mutableStateOf(editingProject?.description ?: "") }
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingProject = null
            },
            title = { Text(if (editingProject == null) "پروژه جدید" else "ویرایش پروژه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام پروژه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("توضیحات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onSaveProject(
                            (editingProject ?: ProjectEntity(name = name.trim())).copy(
                                name = name.trim(),
                                description = desc.trim()
                            )
                        )
                        showCreateDialog = false
                        editingProject = null
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    editingProject = null
                }) { Text("انصراف") }
            }
        )
    }
}

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    onSaveNote: (NoteEntity) -> Unit,
    onTrashNote: (NoteEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    val filtered = remember(notes, query) {
        if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("یادداشت‌ها", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("یادداشت جدید")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("جستجو در یادداشت‌ها...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingNote = note },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (note.content.isNotBlank()) {
                                Text(
                                    note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            }
                        }
                        IconButton(onClick = { onTrashNote(note) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف یادداشت")
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingNote != null) {
        var title by remember(editingNote) { mutableStateOf(editingNote?.title ?: "") }
        var content by remember(editingNote) { mutableStateOf(editingNote?.content ?: "") }
        AlertDialog(
            onDismissRequest = {
                showCreate = false
                editingNote = null
            },
            title = { Text(if (editingNote == null) "یادداشت جدید" else "ویرایش یادداشت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("متن یادداشت") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onSaveNote(
                            (editingNote ?: NoteEntity(title = title.trim(), content = content.trim())).copy(
                                title = title.trim(),
                                content = content.trim()
                            )
                        )
                        showCreate = false
                        editingNote = null
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreate = false
                    editingNote = null
                }) { Text("انصراف") }
            }
        )
    }
}

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("دسته‌بندی‌ها", style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("نام دسته‌بندی جدید") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newName.isNotBlank()) {
                    onSaveCategory(CategoryEntity(name = newName.trim(), sortOrder = categories.size))
                    newName = ""
                }
            }) {
                Text("افزودن")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.name, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onDeleteCategory(cat) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف دسته‌بندی")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(reminders: List<ReminderEntity>) {
    val total = reminders.size
    val completed = reminders.count { it.status == ReminderStatus.COMPLETED.name }
    val active = reminders.count { it.status == ReminderStatus.ACTIVE.name }
    val overdue = reminders.count {
        it.status == ReminderStatus.ACTIVE.name && it.triggerAtMillis < System.currentTimeMillis()
    }
    val rate = if (total == 0) 0f else completed.toFloat() / total.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("آمار و گزارش‌ها", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "نرخ تکمیل کارها: ${PersianCalendarUtils.toPersianDigits((rate * 100).toInt())}٪",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(progress = { rate }, modifier = Modifier.fillMaxWidth())
                Text("کل کارها: ${PersianCalendarUtils.toPersianDigits(total)}")
                Text("انجام‌شده: ${PersianCalendarUtils.toPersianDigits(completed)}")
                Text("فعال: ${PersianCalendarUtils.toPersianDigits(active)}")
                Text("عقب‌افتاده: ${PersianCalendarUtils.toPersianDigits(overdue)}")
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    reminders: List<ReminderEntity>,
    settings: YadetSettings,
    onEditReminder: (ReminderEntity) -> Unit,
    onUncomplete: (ReminderEntity) -> Unit
) {
    val completed = remember(reminders) {
        reminders.filter { it.status == ReminderStatus.COMPLETED.name }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("آرشیو کارهای انجام‌شده", style = MaterialTheme.typography.headlineMedium) }
        if (completed.isEmpty()) {
            item { Text("آرشیو خالی است.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(completed, key = { it.id }) { r ->
                ReminderItemCard(
                    reminder = r,
                    isJalali = settings.calendarSystem == CalendarSystem.JALALI,
                    is24Hour = settings.is24Hour,
                    onClick = { onEditReminder(r) },
                    onToggleComplete = { onUncomplete(r) }
                )
            }
        }
    }
}

@Composable
fun TrashScreen(
    trashedReminders: List<ReminderEntity>,
    onRestore: (ReminderEntity) -> Unit,
    onDeletePermanently: (ReminderEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سطل زباله", style = MaterialTheme.typography.headlineMedium)
            if (trashedReminders.isNotEmpty()) {
                OutlinedButton(onClick = onEmptyTrash) {
                    Text("خالی کردن سطل زباله")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (trashedReminders.isEmpty()) {
                item { Text("سطل زباله خالی است.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(trashedReminders, key = { it.id }) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(onClick = { onRestore(r) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "بازیابی")
                                }
                                IconButton(onClick = { onDeletePermanently(r) }) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        contentDescription = "حذف دائمی",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
