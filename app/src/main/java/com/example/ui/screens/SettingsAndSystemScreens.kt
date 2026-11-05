package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ai.AiModelConfig
import com.example.ai.AiModelRole
import com.example.ai.AiMultiModelConfig
import com.example.ai.AiProviderInfo
import com.example.ai.PricingVerificationStatus
import com.example.backup.BackupManager
import com.example.backup.BackupValidationResult
import com.example.data.local.AiProviderType
import com.example.data.local.AppThemeMode
import com.example.data.local.CalendarSystem
import com.example.data.local.MemoryItemEntity
import com.example.data.local.YadetSettings
import com.example.util.PersianCalendarUtils
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(
    memories: List<MemoryItemEntity>,
    onSaveMemory: (MemoryItemEntity) -> Unit,
    onDeleteMemory: (MemoryItemEntity) -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حافظه دستیار", style = MaterialTheme.typography.headlineMedium)
        Text(
            "اطلاعات و ترجیحات ذخیره‌شده در حافظه بلندمدت دستیار را مشاهده، ویرایش یا حذف کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("عنوان (مثلاً ساعت بیداری)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("مقدار") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (keyText.isNotBlank() && valueText.isNotBlank()) {
                    onSaveMemory(
                        MemoryItemEntity(
                            type = "LONG_TERM",
                            key = keyText.trim(),
                            value = valueText.trim()
                        )
                    )
                    keyText = ""
                    valueText = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن به حافظه")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { item ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(item.value, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onDeleteMemory(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف از حافظه")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: YadetSettings,
    multiModelConfig: AiMultiModelConfig,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateCalendar: (CalendarSystem) -> Unit,
    onUpdateIs24Hour: (Boolean) -> Unit,
    onUpdateNotifications: (Int?, Boolean?, Boolean?, Int?) -> Unit,
    onUpdateVoiceTts: (Boolean) -> Unit,
    onUpdateAiConfig: (AiProviderType?, String?, String?, String?, Boolean?, Int?, Boolean?) -> Unit,
    onSaveMultiModelConfig: (AiMultiModelConfig) -> Unit,
    onSetAllowPaidModels: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onTestAiConnection: () -> Unit,
    onSetAppLock: (Boolean, String?) -> Unit
) {
    var showProviderMenu by remember { mutableStateOf(false) }
    var baseUrlInput by remember(settings.aiBaseUrl) { mutableStateOf(settings.aiBaseUrl) }
    var modelInput by remember(settings.aiModelName) { mutableStateOf(settings.aiModelName) }
    var fallbackInput by remember(settings.aiFallbackModelName) { mutableStateOf(settings.aiFallbackModelName) }
    var apiKeyInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("تنظیمات", style = MaterialTheme.typography.headlineMedium) }

        // Appearance & Calendar
        item {
            SettingsSectionCard("ظاهر و تقویم") {
                Text("تم برنامه:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(mode.persianLabel) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("تقویم پیش‌فرض:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalendarSystem.entries.forEach { cal ->
                        FilterChip(
                            selected = settings.calendarSystem == cal,
                            onClick = { onUpdateCalendar(cal) },
                            label = { Text(cal.persianLabel) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نمایش ساعت ۲۴ ساعته")
                    Switch(checked = settings.is24Hour, onCheckedChange = onUpdateIs24Hour)
                }
            }
        }

        // Notifications
        item {
            SettingsSectionCard("اعلان‌ها و یادآوری") {
                Text("اطلاع‌رسانی زودتر از موعد (دقیقه):", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = settings.defaultLeadMinutes == mins,
                            onClick = { onUpdateNotifications(mins, null, null, null) },
                            label = {
                                Text(
                                    if (mins == 0) "سر وقت"
                                    else "${PersianCalendarUtils.toPersianDigits(mins)} دقیقه زودتر"
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("صدای اعلان")
                    Switch(
                        checked = settings.notificationSound,
                        onCheckedChange = { onUpdateNotifications(null, it, null, null) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("لرزش اعلان")
                    Switch(
                        checked = settings.notificationVibration,
                        onCheckedChange = { onUpdateNotifications(null, null, it, null) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("پخش صوتی پاسخ دستیار (TTS)")
                    Switch(checked = settings.voiceTtsEnabled, onCheckedChange = onUpdateVoiceTts)
                }
            }
        }

        // AI Model Settings (Provider-Agnostic)
        item {
            SettingsSectionCard("مدل هوش مصنوعی (مستقل از ارائه‌دهنده)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فعال‌سازی اتصال آنلاین هوش مصنوعی")
                    Switch(
                        checked = settings.aiOnlineEnabled,
                        onCheckedChange = { onUpdateAiConfig(null, null, null, null, it, null, null) }
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { showProviderMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ارائه‌دهنده: ${settings.aiProvider.persianLabel}")
                    }
                    DropdownMenu(
                        expanded = showProviderMenu,
                        onDismissRequest = { showProviderMenu = false }
                    ) {
                        AiProviderType.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.persianLabel) },
                                onClick = {
                                    showProviderMenu = false
                                    baseUrlInput = p.defaultBaseUrl
                                    modelInput = p.defaultModel
                                    onUpdateAiConfig(p, p.defaultBaseUrl, p.defaultModel, null, null, null, null)
                                }
                            )
                        }
                    }
                }

                if (settings.aiProvider != AiProviderType.NONE) {
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("آدرس سرور (Base URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("نام مدل پیش‌فرض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fallbackInput,
                        onValueChange = { fallbackInput = it },
                        label = { Text("مدل جایگزین (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onUpdateAiConfig(null, baseUrlInput, modelInput, fallbackInput, null, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره تنظیمات مدل")
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = {
                            Text(
                                if (settings.hasApiKey) "کلید API ذخیره شده است (برای تغییر وارد کنید)"
                                else "کلید API شخصی (ذخیره امن در Keystore)"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveApiKey(apiKeyInput)
                                apiKeyInput = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ذخیره کلید")
                        }
                        if (settings.hasApiKey) {
                            OutlinedButton(
                                onClick = { onSaveApiKey("") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حذف کلید")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onTestAiConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("آزمایش اتصال به مدل")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("جستجوی اینترنتی در صورت نیاز")
                        Switch(
                            checked = settings.webSearchEnabled,
                            onCheckedChange = { onUpdateAiConfig(null, null, null, null, null, null, it) }
                        )
                    }
                }
            }
        }

        // Multi-Model & Cost Management
        item {
            SettingsSectionCard("مدیریت هوش مصنوعی (چندمدلی و کنترل هزینه)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("انتخاب خودکار هوشمند مدل")
                    Switch(
                        checked = multiModelConfig.autoRoutingEnabled,
                        onCheckedChange = {
                            onSaveMultiModelConfig(multiModelConfig.copy(autoRoutingEnabled = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اجازه استفاده از مدل‌های پولی / تأییدنشده")
                    Switch(
                        checked = multiModelConfig.allowPaidModels,
                        onCheckedChange = onSetAllowPaidModels
                    )
                }

                val stats = multiModelConfig.usageStats
                Text(
                    text = "مصرف امروز: ${PersianCalendarUtils.toPersianDigits(stats.dailyRequestCount)} از ${PersianCalendarUtils.toPersianDigits(stats.dailyRequestLimit)} درخواست | ماهانه: ${PersianCalendarUtils.toPersianDigits(stats.monthlyRequestCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text("مدل‌های ثبت‌شده:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                multiModelConfig.models.forEach { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val badges = buildList {
                                add(model.pricingStatus.persianLabel)
                                add(if (model.isLocal) "محلی" else "ابری")
                                if (model.supportsToolCalling) add("پشتیبانی از ابزار")
                            }.joinToString(" • ")
                            Text(badges, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = model.isEnabled,
                            onCheckedChange = { enabled ->
                                val updated = multiModelConfig.models.map {
                                    if (it.id == model.id) it.copy(isEnabled = enabled) else it
                                }
                                onSaveMultiModelConfig(multiModelConfig.copy(models = updated))
                            }
                        )
                    }
                }
            }
        }

        // Security / App Lock
        item {
            SettingsSectionCard("امنیت و حریم خصوصی") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قفل برنامه با رمز PIN")
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                onSetAppLock(false, null)
                            } else if (pinInput.length >= 4) {
                                onSetAppLock(true, pinInput)
                                pinInput = ""
                            }
                        }
                    )
                }
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 8) pinInput = it },
                    label = { Text("رمز PIN جدید (حداقل ۴ رقم)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            onSetAppLock(true, pinInput)
                            pinInput = ""
                        }
                    },
                    enabled = pinInput.length >= 4
                ) {
                    Text("تنظیم و فعال‌سازی رمز")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun BackupScreen(
    onExportBackup: suspend (String?) -> String,
    onValidateBackup: (String, String?) -> BackupValidationResult,
    onRestoreBackup: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var pendingRestoreContent by remember { mutableStateOf<String?>(null) }
    var validationResult by remember { mutableStateOf<BackupValidationResult?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                pendingRestoreContent = content
                validationResult = onValidateBackup(content, importPassword.ifBlank { null })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("پشتیبان‌گیری و بازیابی", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تهیه نسخه پشتیبان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = exportPassword,
                    onValueChange = { exportPassword = it },
                    label = { Text("رمز عبور رمزنگاری (اختیاری - AES-256)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        scope.launch {
                            val json = onExportBackup(exportPassword.ifBlank { null })
                            val file = BackupManager.createShareableBackupFile(context, json)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "ذخیره یا اشتراک‌گذاری پشتیبان یادت"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تهیه و اشتراک‌گذاری فایل پشتیبان")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("بازیابی از فایل پشتیبان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = { Text("رمز عبور فایل (در صورت رمزنگاری)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انتخاب فایل پشتیبان")
                }
            }
        }
    }

    if (pendingRestoreContent != null && validationResult != null) {
        val res = validationResult!!
        AlertDialog(
            onDismissRequest = {
                pendingRestoreContent = null
                validationResult = null
            },
            title = { Text(if (res.isValid) "تأیید بازیابی اطلاعات" else "خطا در اعتبارسنجی") },
            text = {
                if (res.isValid) {
                    Text(
                        "فایل پشتیبان معتبر است (${PersianCalendarUtils.toPersianDigits(res.reminderCount)} یادآور، ${PersianCalendarUtils.toPersianDigits(res.projectCount)} پروژه، ${PersianCalendarUtils.toPersianDigits(res.noteCount)} یادداشت).\nآیا مطمئن هستید که می‌خواهید اطلاعات فعلی جایگزین شوند؟"
                    )
                } else {
                    Text(res.errorMessage ?: "فایل نامعتبر است.")
                }
            },
            confirmButton = {
                if (res.isValid) {
                    TextButton(onClick = {
                        onRestoreBackup(pendingRestoreContent!!, importPassword.ifBlank { null })
                        pendingRestoreContent = null
                        validationResult = null
                    }) {
                        Text("بله، بازیابی شود")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestoreContent = null
                    validationResult = null
                }) {
                    Text("بستن")
                }
            }
        )
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("درباره یادت (Yadet)", style = MaterialTheme.typography.headlineMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نسخه: ۱.۰ (Yadet 1.0)", fontWeight = FontWeight.Bold)
                Text("دستیار هوش مصنوعی فارسی، مدیریت هوشمند زندگی، تقویم شمسی و یادآورها.")
                Text("• ذخیره‌سازی محلی امن با Room و DataStore")
                Text("• حفاظت از کلیدهای API با Android Keystore")
                Text("• معماری مستقل از ارائه‌دهنده هوش مصنوعی (Provider-Agnostic)")
                Text("• موتور اعلان دقیق با AlarmManager و پشتیبانی از تکرارهای پیشرفته")
            }
        }
    }
}
