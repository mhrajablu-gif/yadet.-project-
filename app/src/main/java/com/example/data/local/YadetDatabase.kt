package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ReminderEntity::class,
        ProjectEntity::class,
        NoteEntity::class,
        CategoryEntity::class,
        ChatMessageEntity::class,
        MemoryItemEntity::class,
        ToolExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class YadetDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun projectDao(): ProjectDao
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun toolLogDao(): ToolLogDao

    companion object {
        @Volatile
        private var INSTANCE: YadetDatabase? = null

        fun getInstance(context: Context): YadetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YadetDatabase::class.java,
                    "yadet_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val catDao = getInstance(context).categoryDao()
                                if (catDao.getAllOnce().isEmpty()) {
                                    catDao.insertAll(
                                        listOf(
                                            CategoryEntity(name = "شخصی", colorHex = "#36A9C9", iconName = "person", sortOrder = 0),
                                            CategoryEntity(name = "کاری", colorHex = "#1D3557", iconName = "work", sortOrder = 1),
                                            CategoryEntity(name = "سلامتی", colorHex = "#2A9D8F", iconName = "favorite", sortOrder = 2),
                                            CategoryEntity(name = "مالی", colorHex = "#E9C46A", iconName = "payments", sortOrder = 3),
                                            CategoryEntity(name = "آموزش", colorHex = "#8E7DBE", iconName = "school", sortOrder = 4)
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
