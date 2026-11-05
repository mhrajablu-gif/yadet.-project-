package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.RecurrenceType

enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    DISABLED,
    TRASHED
}

enum class ReminderPriority(val level: Int, val persianLabel: String) {
    LOW(0, "کم"),
    NORMAL(1, "عادی"),
    HIGH(2, "مهم"),
    URGENT(3, "فوری");

    companion object {
        fun fromLevel(level: Int): ReminderPriority =
            entries.find { it.level == level } ?: NORMAL
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerAtMillis: Long,
    val timezoneId: String = "Asia/Tehran",
    val priority: Int = ReminderPriority.NORMAL.level,
    val categoryId: Long? = null,
    val projectId: Long? = null,
    val status: String = ReminderStatus.ACTIVE.name,
    val recurrenceType: String = RecurrenceType.NONE.name,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysMask: Int = 0,
    val recurrenceStartMillis: Long? = null,
    val recurrenceEndMillis: Long? = null,
    val location: String = "",
    val url: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#36A9C9",
    val iconName: String = "folder",
    val isTrashed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val categoryId: Long? = null,
    val projectId: Long? = null,
    val isTrashed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#36A9C9",
    val iconName: String = "label",
    val sortOrder: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // USER, ASSISTANT, TOOL
    val content: String,
    val toolName: String? = null,
    val toolResultJson: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // SHORT_TERM, LONG_TERM
    val key: String,
    val value: String,
    val category: String = "عمومی",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_execution_logs")
data class ToolExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String,
    val success: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)
