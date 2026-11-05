package com.example.data.repository

import android.content.Context
import com.example.alarm.ReminderAlarmManager
import com.example.data.local.CategoryEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderStatus
import com.example.data.local.SettingsDataStore
import com.example.data.local.ToolExecutionLogEntity
import com.example.data.local.YadetDatabase
import com.example.util.PersianCalendarUtils
import com.example.util.RecurrenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.TimeZone

class YadetRepository(
    private val context: Context,
    private val db: YadetDatabase,
    val settingsStore: SettingsDataStore
) {
    val remindersFlow: Flow<List<ReminderEntity>> = db.reminderDao().observeAllNonTrashed()
    val trashedRemindersFlow: Flow<List<ReminderEntity>> = db.reminderDao().observeTrashed()
    val projectsFlow: Flow<List<ProjectEntity>> = db.projectDao().observeActive()
    val trashedProjectsFlow: Flow<List<ProjectEntity>> = db.projectDao().observeTrashed()
    val notesFlow: Flow<List<NoteEntity>> = db.noteDao().observeActive()
    val trashedNotesFlow: Flow<List<NoteEntity>> = db.noteDao().observeTrashed()
    val categoriesFlow: Flow<List<CategoryEntity>> = db.categoryDao().observeAll()
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = db.chatMessageDao().observeAll()
    val memoryItemsFlow: Flow<List<MemoryItemEntity>> = db.memoryDao().observeAll()
    val toolLogsFlow: Flow<List<ToolExecutionLogEntity>> = db.toolLogDao().observeRecent()

    suspend fun createReminder(reminder: ReminderEntity): Long {
        val now = System.currentTimeMillis()
        val toInsert = reminder.copy(createdAtMillis = now, updatedAtMillis = now)
        val id = db.reminderDao().insert(toInsert)
        val inserted = toInsert.copy(id = id)
        val settings = settingsStore.settingsFlow.first()
        ReminderAlarmManager.scheduleReminder(context, inserted, settings.defaultLeadMinutes)
        return id
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        val updated = reminder.copy(updatedAtMillis = System.currentTimeMillis())
        db.reminderDao().update(updated)
        val settings = settingsStore.settingsFlow.first()
        ReminderAlarmManager.scheduleReminder(context, updated, settings.defaultLeadMinutes)
    }

    suspend fun completeReminder(reminder: ReminderEntity) {
        val recType = runCatching {
            RecurrenceType.valueOf(reminder.recurrenceType)
        }.getOrDefault(RecurrenceType.NONE)

        val nextMillis = PersianCalendarUtils.calculateNextOccurrence(
            currentTriggerMillis = reminder.triggerAtMillis,
            recurrenceType = recType,
            interval = reminder.recurrenceInterval,
            daysMask = reminder.recurrenceDaysMask,
            recurrenceEndMillis = reminder.recurrenceEndMillis,
            timeZone = TimeZone.getTimeZone(reminder.timezoneId)
        )

        if (nextMillis != null) {
            val updated = reminder.copy(
                triggerAtMillis = nextMillis,
                status = ReminderStatus.ACTIVE.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.reminderDao().update(updated)
            val settings = settingsStore.settingsFlow.first()
            ReminderAlarmManager.scheduleReminder(context, updated, settings.defaultLeadMinutes)
        } else {
            val updated = reminder.copy(
                status = ReminderStatus.COMPLETED.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.reminderDao().update(updated)
            ReminderAlarmManager.cancelReminder(context, reminder.id)
        }
    }

    suspend fun uncompleteReminder(reminder: ReminderEntity) {
        val updated = reminder.copy(
            status = ReminderStatus.ACTIVE.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.reminderDao().update(updated)
        val settings = settingsStore.settingsFlow.first()
        ReminderAlarmManager.scheduleReminder(context, updated, settings.defaultLeadMinutes)
    }

    suspend fun toggleReminderEnabled(reminder: ReminderEntity) {
        val newStatus = if (reminder.status == ReminderStatus.DISABLED.name) {
            ReminderStatus.ACTIVE.name
        } else {
            ReminderStatus.DISABLED.name
        }
        val updated = reminder.copy(
            status = newStatus,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.reminderDao().update(updated)
        val settings = settingsStore.settingsFlow.first()
        ReminderAlarmManager.scheduleReminder(context, updated, settings.defaultLeadMinutes)
    }

    suspend fun moveReminderToTrash(reminder: ReminderEntity) {
        val updated = reminder.copy(
            status = ReminderStatus.TRASHED.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.reminderDao().update(updated)
        ReminderAlarmManager.cancelReminder(context, reminder.id)
    }

    suspend fun restoreReminderFromTrash(reminder: ReminderEntity) {
        val updated = reminder.copy(
            status = ReminderStatus.ACTIVE.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.reminderDao().update(updated)
        val settings = settingsStore.settingsFlow.first()
        ReminderAlarmManager.scheduleReminder(context, updated, settings.defaultLeadMinutes)
    }

    suspend fun deleteReminderPermanently(reminder: ReminderEntity) {
        ReminderAlarmManager.cancelReminder(context, reminder.id)
        db.reminderDao().delete(reminder)
    }

    suspend fun emptyReminderTrash() {
        db.reminderDao().emptyTrash()
    }

    suspend fun getReminderById(id: Long): ReminderEntity? = db.reminderDao().getById(id)

    suspend fun searchReminders(query: String): List<ReminderEntity> = db.reminderDao().search(query)

    suspend fun rescheduleAllActiveAlarms() {
        val settings = settingsStore.settingsFlow.first()
        val active = db.reminderDao().getAllActiveFuture(System.currentTimeMillis())
        active.forEach { ReminderAlarmManager.scheduleReminder(context, it, settings.defaultLeadMinutes) }
    }

    // Projects
    suspend fun saveProject(project: ProjectEntity): Long {
        val now = System.currentTimeMillis()
        return if (project.id == 0L) {
            db.projectDao().insert(project.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            db.projectDao().update(project.copy(updatedAtMillis = now))
            project.id
        }
    }

    suspend fun trashProject(project: ProjectEntity) {
        db.projectDao().update(project.copy(isTrashed = true, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun restoreProject(project: ProjectEntity) {
        db.projectDao().update(project.copy(isTrashed = false, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteProjectPermanently(project: ProjectEntity) {
        db.projectDao().delete(project)
    }

    // Notes
    suspend fun saveNote(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        return if (note.id == 0L) {
            db.noteDao().insert(note.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            db.noteDao().update(note.copy(updatedAtMillis = now))
            note.id
        }
    }

    suspend fun trashNote(note: NoteEntity) {
        db.noteDao().update(note.copy(isTrashed = true, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun restoreNote(note: NoteEntity) {
        db.noteDao().update(note.copy(isTrashed = false, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteNotePermanently(note: NoteEntity) {
        db.noteDao().delete(note)
    }

    // Categories
    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            db.categoryDao().insert(category)
        } else {
            db.categoryDao().update(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        db.categoryDao().delete(category)
    }

    // Chat & Memory
    suspend fun addChatMessage(
        role: String,
        content: String,
        toolName: String? = null,
        toolResultJson: String? = null
    ): Long {
        return db.chatMessageDao().insert(
            ChatMessageEntity(
                role = role,
                content = content,
                toolName = toolName,
                toolResultJson = toolResultJson
            )
        )
    }

    suspend fun clearChatHistory() {
        db.chatMessageDao().clearAll()
    }

    suspend fun saveMemory(item: MemoryItemEntity): Long {
        return db.memoryDao().insert(item.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(item: MemoryItemEntity) {
        db.memoryDao().delete(item)
    }

    suspend fun clearMemoryByType(type: String) {
        db.memoryDao().clearByType(type)
    }

    suspend fun logToolExecution(
        toolName: String,
        argumentsJson: String,
        resultJson: String,
        success: Boolean
    ) {
        db.toolLogDao().insert(
            ToolExecutionLogEntity(
                toolName = toolName,
                argumentsJson = argumentsJson,
                resultJson = resultJson,
                success = success
            )
        )
    }
}
