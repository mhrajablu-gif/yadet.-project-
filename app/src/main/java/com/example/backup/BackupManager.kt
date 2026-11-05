package com.example.backup

import android.content.Context
import android.util.Base64
import com.example.data.local.AppThemeMode
import com.example.data.local.CalendarSystem
import com.example.data.local.CategoryEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.YadetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupValidationResult(
    val isValid: Boolean,
    val isEncrypted: Boolean = false,
    val reminderCount: Int = 0,
    val projectCount: Int = 0,
    val noteCount: Int = 0,
    val errorMessage: String? = null
)

object BackupManager {

    private const val BACKUP_VERSION = 1

    suspend fun exportBackupJson(
        db: YadetDatabase,
        settingsStore: SettingsDataStore,
        password: String? = null
    ): String = withContext(Dispatchers.IO) {
        val reminders = db.reminderDao().getAllOnce()
        val projects = db.projectDao().getAllOnce()
        val notes = db.noteDao().getAllOnce()
        val categories = db.categoryDao().getAllOnce()
        val memories = db.memoryDao().getAllOnce()
        val settings = settingsStore.settingsFlow.first()

        val payload = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("description", r.description)
                        put("triggerAtMillis", r.triggerAtMillis)
                        put("timezoneId", r.timezoneId)
                        put("priority", r.priority)
                        put("categoryId", r.categoryId)
                        put("projectId", r.projectId)
                        put("status", r.status)
                        put("recurrenceType", r.recurrenceType)
                        put("recurrenceInterval", r.recurrenceInterval)
                        put("recurrenceDaysMask", r.recurrenceDaysMask)
                        put("recurrenceStartMillis", r.recurrenceStartMillis)
                        put("recurrenceEndMillis", r.recurrenceEndMillis)
                        put("location", r.location)
                        put("url", r.url)
                        put("createdAtMillis", r.createdAtMillis)
                        put("updatedAtMillis", r.updatedAtMillis)
                    })
                }
            })
            put("projects", JSONArray().apply {
                projects.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description)
                        put("colorHex", p.colorHex)
                        put("iconName", p.iconName)
                        put("isTrashed", p.isTrashed)
                        put("createdAtMillis", p.createdAtMillis)
                        put("updatedAtMillis", p.updatedAtMillis)
                    })
                }
            })
            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        put("categoryId", n.categoryId)
                        put("projectId", n.projectId)
                        put("isTrashed", n.isTrashed)
                        put("createdAtMillis", n.createdAtMillis)
                        put("updatedAtMillis", n.updatedAtMillis)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("colorHex", c.colorHex)
                        put("iconName", c.iconName)
                        put("sortOrder", c.sortOrder)
                    })
                }
            })
            put("memories", JSONArray().apply {
                memories.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("type", m.type)
                        put("key", m.key)
                        put("value", m.value)
                        put("category", m.category)
                        put("updatedAtMillis", m.updatedAtMillis)
                    })
                }
            })
            put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("calendarSystem", settings.calendarSystem.name)
                put("is24Hour", settings.is24Hour)
                put("defaultLeadMinutes", settings.defaultLeadMinutes)
                put("notificationSound", settings.notificationSound)
                put("notificationVibration", settings.notificationVibration)
                put("defaultSnoozeMinutes", settings.defaultSnoozeMinutes)
                put("voiceTtsEnabled", settings.voiceTtsEnabled)
            })
        }

        val rawPayload = payload.toString()
        val checksum = sha256(rawPayload)

        if (!password.isNullOrBlank()) {
            val (saltB64, ivB64, cipherB64) = encryptWithPassword(rawPayload, password)
            JSONObject().apply {
                put("yadetBackup", true)
                put("encrypted", true)
                put("salt", saltB64)
                put("iv", ivB64)
                put("checksum", checksum)
                put("data", cipherB64)
            }.toString(2)
        } else {
            JSONObject().apply {
                put("yadetBackup", true)
                put("encrypted", false)
                put("checksum", checksum)
                put("payload", payload)
            }.toString(2)
        }
    }

    fun validateBackup(rawContent: String, password: String? = null): BackupValidationResult {
        return try {
            val root = JSONObject(rawContent)
            if (!root.optBoolean("yadetBackup", false)) {
                return BackupValidationResult(false, errorMessage = "فایل انتخاب‌شده پشتیبان معتبر یادت نیست.")
            }
            val encrypted = root.optBoolean("encrypted", false)
            val expectedChecksum = root.optString("checksum", "")
            if (encrypted) {
                if (password.isNullOrBlank()) {
                    return BackupValidationResult(
                        isValid = false,
                        isEncrypted = true,
                        errorMessage = "این فایل پشتیبان رمزنگاری شده است. لطفاً رمز عبور را وارد کنید."
                    )
                }
                val decrypted = decryptWithPassword(
                    saltB64 = root.getString("salt"),
                    ivB64 = root.getString("iv"),
                    cipherB64 = root.getString("data"),
                    password = password
                )
                if (sha256(decrypted) != expectedChecksum) {
                    return BackupValidationResult(
                        isValid = false,
                        isEncrypted = true,
                        errorMessage = "رمز عبور اشتباه است یا فایل آسیب دیده است."
                    )
                }
                val payload = JSONObject(decrypted)
                BackupValidationResult(
                    isValid = true,
                    isEncrypted = true,
                    reminderCount = payload.optJSONArray("reminders")?.length() ?: 0,
                    projectCount = payload.optJSONArray("projects")?.length() ?: 0,
                    noteCount = payload.optJSONArray("notes")?.length() ?: 0
                )
            } else {
                val payload = root.getJSONObject("payload")
                if (sha256(payload.toString()) != expectedChecksum) {
                    return BackupValidationResult(
                        isValid = false,
                        errorMessage = "یکپارچگی فایل پشتیبان تأیید نشد (Checksum نامعتبر)."
                    )
                }
                BackupValidationResult(
                    isValid = true,
                    isEncrypted = false,
                    reminderCount = payload.optJSONArray("reminders")?.length() ?: 0,
                    projectCount = payload.optJSONArray("projects")?.length() ?: 0,
                    noteCount = payload.optJSONArray("notes")?.length() ?: 0
                )
            }
        } catch (e: Exception) {
            BackupValidationResult(false, errorMessage = "خطا در خواندن فایل پشتیبان: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackup(
        rawContent: String,
        db: YadetDatabase,
        settingsStore: SettingsDataStore,
        password: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val validation = validateBackup(rawContent, password)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException(validation.errorMessage ?: "فایل نامعتبر"))
            }
            val root = JSONObject(rawContent)
            val payload = if (root.optBoolean("encrypted", false)) {
                val decrypted = decryptWithPassword(
                    root.getString("salt"),
                    root.getString("iv"),
                    root.getString("data"),
                    password!!
                )
                JSONObject(decrypted)
            } else {
                root.getJSONObject("payload")
            }

            val reminders = mutableListOf<ReminderEntity>()
            val remArr = payload.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remArr.length()) {
                val o = remArr.getJSONObject(i)
                reminders.add(
                    ReminderEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        triggerAtMillis = o.getLong("triggerAtMillis"),
                        timezoneId = o.optString("timezoneId", "Asia/Tehran"),
                        priority = o.optInt("priority", 1),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        projectId = if (o.isNull("projectId")) null else o.getLong("projectId"),
                        status = o.optString("status", "ACTIVE"),
                        recurrenceType = o.optString("recurrenceType", "NONE"),
                        recurrenceInterval = o.optInt("recurrenceInterval", 1),
                        recurrenceDaysMask = o.optInt("recurrenceDaysMask", 0),
                        recurrenceStartMillis = if (o.isNull("recurrenceStartMillis")) null else o.getLong("recurrenceStartMillis"),
                        recurrenceEndMillis = if (o.isNull("recurrenceEndMillis")) null else o.getLong("recurrenceEndMillis"),
                        location = o.optString("location", ""),
                        url = o.optString("url", ""),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }

            val projects = mutableListOf<ProjectEntity>()
            val projArr = payload.optJSONArray("projects") ?: JSONArray()
            for (i in 0 until projArr.length()) {
                val o = projArr.getJSONObject(i)
                projects.add(
                    ProjectEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        description = o.optString("description", ""),
                        colorHex = o.optString("colorHex", "#36A9C9"),
                        iconName = o.optString("iconName", "folder"),
                        isTrashed = o.optBoolean("isTrashed", false),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }

            val notes = mutableListOf<NoteEntity>()
            val noteArr = payload.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until noteArr.length()) {
                val o = noteArr.getJSONObject(i)
                notes.add(
                    NoteEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        content = o.optString("content", ""),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        projectId = if (o.isNull("projectId")) null else o.getLong("projectId"),
                        isTrashed = o.optBoolean("isTrashed", false),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }

            val categories = mutableListOf<CategoryEntity>()
            val catArr = payload.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        colorHex = o.optString("colorHex", "#36A9C9"),
                        iconName = o.optString("iconName", "label"),
                        sortOrder = o.optInt("sortOrder", i)
                    )
                )
            }

            val memories = mutableListOf<MemoryItemEntity>()
            val memArr = payload.optJSONArray("memories") ?: JSONArray()
            for (i in 0 until memArr.length()) {
                val o = memArr.getJSONObject(i)
                memories.add(
                    MemoryItemEntity(
                        id = o.getLong("id"),
                        type = o.optString("type", "LONG_TERM"),
                        key = o.getString("key"),
                        value = o.getString("value"),
                        category = o.optString("category", "عمومی"),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }

            db.reminderDao().deleteAll()
            db.projectDao().deleteAll()
            db.noteDao().deleteAll()
            db.categoryDao().deleteAll()
            db.memoryDao().deleteAll()

            db.categoryDao().insertAll(categories)
            db.projectDao().insertAll(projects)
            db.noteDao().insertAll(notes)
            db.reminderDao().insertAll(reminders)
            db.memoryDao().insertAll(memories)

            payload.optJSONObject("settings")?.let { s ->
                runCatching { AppThemeMode.valueOf(s.optString("themeMode")) }
                    .getOrNull()?.let { settingsStore.updateThemeMode(it) }
                runCatching { CalendarSystem.valueOf(s.optString("calendarSystem")) }
                    .getOrNull()?.let { settingsStore.updateCalendarSystem(it) }
                settingsStore.updateIs24Hour(s.optBoolean("is24Hour", true))
                settingsStore.updateNotificationSettings(
                    leadMinutes = s.optInt("defaultLeadMinutes", 0),
                    sound = s.optBoolean("notificationSound", true),
                    vibration = s.optBoolean("notificationVibration", true),
                    snoozeMinutes = s.optInt("defaultSnoozeMinutes", 10)
                )
                settingsStore.updateVoiceTts(s.optBoolean("voiceTtsEnabled", false))
            }

            Result.success("بازیابی با موفقیت انجام شد (${reminders.size} یادآور، ${projects.size} پروژه، ${notes.size} یادداشت).")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createShareableBackupFile(context: Context, content: String): File {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "yadet-backup-${System.currentTimeMillis()}.yadet")
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun encryptWithPassword(plainText: String, password: String): Triple<String, String, String> {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return Triple(
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        )
    }

    private fun decryptWithPassword(
        saltB64: String,
        ivB64: String,
        cipherB64: String,
        password: String
    ): String {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val encrypted = Base64.decode(cipherB64, Base64.NO_WRAP)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
