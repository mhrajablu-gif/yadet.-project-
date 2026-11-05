package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status != 'TRASHED' ORDER BY triggerAtMillis ASC")
    fun observeAllNonTrashed(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'TRASHED' ORDER BY updatedAtMillis DESC")
    fun observeTrashed(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND triggerAtMillis > :nowMillis")
    suspend fun getAllActiveFuture(nowMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status != 'TRASHED' AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY triggerAtMillis ASC")
    suspend fun search(query: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders WHERE status = 'TRASHED'")
    suspend fun emptyTrash()

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isTrashed = 0 ORDER BY createdAtMillis DESC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isTrashed = 1 ORDER BY updatedAtMillis DESC")
    fun observeTrashed(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllOnce(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 ORDER BY updatedAtMillis DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAtMillis DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllOnce(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items ORDER BY updatedAtMillis DESC")
    suspend fun getAllOnce(): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MemoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MemoryItemEntity>)

    @Update
    suspend fun update(item: MemoryItemEntity)

    @Delete
    suspend fun delete(item: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE type = :type")
    suspend fun clearByType(type: String)

    @Query("DELETE FROM memory_items")
    suspend fun deleteAll()
}

@Dao
interface ToolLogDao {
    @Query("SELECT * FROM tool_execution_logs ORDER BY timestampMillis DESC LIMIT 100")
    fun observeRecent(): Flow<List<ToolExecutionLogEntity>>

    @Insert
    suspend fun insert(log: ToolExecutionLogEntity): Long
}
