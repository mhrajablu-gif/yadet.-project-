package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.YadetApplication
import com.example.ai.AiModelConfig
import com.example.ai.AiMultiModelConfig
import com.example.ai.AiProviderInfo
import com.example.backup.BackupManager
import com.example.backup.BackupValidationResult
import com.example.data.local.AiProviderType
import com.example.data.local.AppThemeMode
import com.example.data.local.CalendarSystem
import com.example.data.local.CategoryEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ToolExecutionLogEntity
import com.example.data.local.YadetSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class YadetDestination(val persianTitle: String) {
    DASHBOARD("داشبورد"),
    ALL_TASKS("همه کارها"),
    CALENDAR("تقویم"),
    NOTES("یادداشت‌ها"),
    PROJECTS("پروژه‌ها"),
    CATEGORIES("دسته‌بندی‌ها"),
    STATISTICS("آمار و گزارش‌ها"),
    ARCHIVE("آرشیو"),
    TRASH("سطل زباله"),
    AI_CHAT("دستیار هوش مصنوعی"),
    MEMORY("حافظه دستیار"),
    SETTINGS("تنظیمات"),
    BACKUP("پشتیبان‌گیری و بازیابی"),
    ABOUT("درباره برنامه")
}

enum class StatsRange(val persianLabel: String) {
    TODAY("امروز"),
    WEEK("هفته"),
    MONTH("ماه")
}

class YadetViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as YadetApplication
    val repository = app.repository
    val settingsStore = app.settingsDataStore
    val aiClient = app.aiClient
    val aiManager = app.aiManager

    val settings: StateFlow<YadetSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, YadetSettings())

    val multiModelConfig: StateFlow<AiMultiModelConfig> = settingsStore.multiModelConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiMultiModelConfig())

    val reminders: StateFlow<List<ReminderEntity>> = repository.remindersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trashedReminders: StateFlow<List<ReminderEntity>> = repository.trashedRemindersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.notesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val memoryItems: StateFlow<List<MemoryItemEntity>> = repository.memoryItemsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val toolLogs: StateFlow<List<ToolExecutionLogEntity>> = repository.toolLogsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentDestination = MutableStateFlow(YadetDestination.DASHBOARD)
    val currentDestination: StateFlow<YadetDestination> = _currentDestination.asStateFlow()

    private val _statsRange = MutableStateFlow(StatsRange.TODAY)
    val statsRange: StateFlow<StatsRange> = _statsRange.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun navigateTo(dest: YadetDestination) {
        _currentDestination.value = dest
    }

    fun setStatsRange(range: StatsRange) {
        _statsRange.value = range
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = settingsStore.verifyPin(pin, settings.value.appLockPinHash)
        if (ok) _isUnlocked.value = true
        return ok
    }

    // Reminder CRUD
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            if (reminder.id == 0L) {
                repository.createReminder(reminder)
            } else {
                repository.updateReminder(reminder)
            }
        }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.completeReminder(reminder) }
    }

    fun uncompleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.uncompleteReminder(reminder) }
    }

    fun toggleReminderEnabled(reminder: ReminderEntity) {
        viewModelScope.launch { repository.toggleReminderEnabled(reminder) }
    }

    fun trashReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.moveReminderToTrash(reminder) }
    }

    fun restoreReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.restoreReminderFromTrash(reminder) }
    }

    fun deleteReminderPermanently(reminder: ReminderEntity) {
        viewModelScope.launch { repository.deleteReminderPermanently(reminder) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyReminderTrash() }
    }

    // Projects, Notes, Categories
    fun saveProject(project: ProjectEntity) {
        viewModelScope.launch { repository.saveProject(project) }
    }

    fun trashProject(project: ProjectEntity) {
        viewModelScope.launch { repository.trashProject(project) }
    }

    fun saveNote(note: NoteEntity) {
        viewModelScope.launch { repository.saveNote(note) }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch { repository.trashNote(note) }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.saveCategory(category) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    // AI Chat
    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isAiThinking.value) return
        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                repository.addChatMessage("USER", trimmed)
                val currentSettings = settingsStore.settingsFlow.first()
                val apiKey = settingsStore.getDecryptedApiKey()
                val history = chatMessages.value
                val memories = memoryItems.value

                val response = aiManager.sendMessage(
                    userMessage = trimmed,
                    history = history,
                    memories = memories,
                    legacySettings = currentSettings,
                    apiKey = apiKey
                )

                val toolSummary = if (response.executedTools.isNotEmpty()) {
                    response.executedTools.joinToString(" • ")
                } else null

                repository.addChatMessage(
                    role = "ASSISTANT",
                    content = response.text,
                    toolName = toolSummary
                )
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChatHistory() }
    }

    // Memory
    fun saveMemoryItem(item: MemoryItemEntity) {
        viewModelScope.launch { repository.saveMemory(item) }
    }

    fun deleteMemoryItem(item: MemoryItemEntity) {
        viewModelScope.launch { repository.deleteMemory(item) }
    }

    // Settings
    fun updateTheme(mode: AppThemeMode) {
        viewModelScope.launch { settingsStore.updateThemeMode(mode) }
    }

    fun updateCalendar(system: CalendarSystem) {
        viewModelScope.launch { settingsStore.updateCalendarSystem(system) }
    }

    fun updateIs24Hour(is24: Boolean) {
        viewModelScope.launch { settingsStore.updateIs24Hour(is24) }
    }

    fun updateNotificationSettings(lead: Int?, sound: Boolean?, vibration: Boolean?, snooze: Int?) {
        viewModelScope.launch {
            settingsStore.updateNotificationSettings(lead, sound, vibration, snooze)
            repository.rescheduleAllActiveAlarms()
        }
    }

    fun updateVoiceTts(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateVoiceTts(enabled) }
    }

    fun updateAiConfig(
        provider: AiProviderType? = null,
        baseUrl: String? = null,
        modelName: String? = null,
        fallbackModelName: String? = null,
        onlineEnabled: Boolean? = null,
        maxTokens: Int? = null,
        webSearchEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsStore.updateAiConfig(
                provider, baseUrl, modelName, fallbackModelName, onlineEnabled, maxTokens, webSearchEnabled
            )
        }
    }

    fun saveMultiModelConfig(config: AiMultiModelConfig) {
        viewModelScope.launch {
            settingsStore.saveMultiModelConfig(config)
        }
    }

    fun setAllowPaidModels(allow: Boolean) {
        viewModelScope.launch {
            settingsStore.setAllowPaidModels(allow)
            _statusBanner.value = if (allow) {
                "استفاده از مدل‌های پولی با تأیید شما فعال شد."
            } else {
                "قفل مدل‌های پولی فعال شد (فقط مدل‌های رایگان مجاز هستند)."
            }
        }
    }

    fun saveApiKey(plainKey: String) {
        viewModelScope.launch {
            settingsStore.saveApiKey(plainKey)
            _statusBanner.value = if (plainKey.isBlank()) "کلید API حذف شد." else "کلید API با رمزنگاری Keystore ذخیره شد."
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            val s = settingsStore.settingsFlow.first()
            val key = settingsStore.getDecryptedApiKey()
            val res = aiClient.testConnection(s, key)
            _statusBanner.value = res.getOrElse { it.message ?: "خطا در اتصال" }
        }
    }

    fun setAppLock(enabled: Boolean, pin: String?) {
        viewModelScope.launch {
            settingsStore.setAppLock(enabled, pin)
            if (!enabled) _isUnlocked.value = true
            _statusBanner.value = if (enabled) "قفل برنامه فعال شد." else "قفل برنامه غیرفعال شد."
        }
    }

    // Backup & Restore
    suspend fun exportBackup(password: String?): String {
        return BackupManager.exportBackupJson(app.database, settingsStore, password)
    }

    fun validateBackup(raw: String, password: String?): BackupValidationResult {
        return BackupManager.validateBackup(raw, password)
    }

    fun restoreBackup(raw: String, password: String?) {
        viewModelScope.launch {
            val res = BackupManager.restoreBackup(raw, app.database, settingsStore, password)
            _statusBanner.value = res.getOrElse { "خطا در بازیابی: ${it.localizedMessage}" }
            repository.rescheduleAllActiveAlarms()
        }
    }
}
