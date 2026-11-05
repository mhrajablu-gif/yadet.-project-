package com.example.ai

import android.app.ActivityManager
import android.content.Context
import com.example.data.local.AiProviderType
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.YadetSettings
import com.example.util.PersianCalendarUtils
import kotlinx.coroutines.flow.first

class AiManager(
    private val context: Context,
    private val providerClient: AiProviderClient,
    private val settingsStore: SettingsDataStore
) {

    fun isDeviceCapableForLocalModel(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem >= 5_500_000_000L
    }

    suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessageEntity>,
        memories: List<MemoryItemEntity>,
        legacySettings: YadetSettings,
        apiKey: String
    ): AiResponse {
        val multiConfig = settingsStore.multiModelConfigFlow.first()

        // Check usage limits
        val stats = multiConfig.usageStats
        if (stats.dailyRequestCount >= stats.dailyRequestLimit) {
            return AiResponse(
                text = "سقف تعداد درخواست روزانه (${PersianCalendarUtils.toPersianDigits(stats.dailyRequestLimit)} درخواست) به پایان رسیده است. برای جلوگیری از هزینه ناخواسته، ارسال درخواست متوقف شد.",
                isError = true
            )
        }

        // Build ordered candidate list
        val candidates = buildCandidateModels(multiConfig)
        if (candidates.isEmpty()) {
            return AiResponse(
                text = "هیچ مدل هوش مصنوعی فعال و مجازی یافت نشد. لطفاً در بخش «تنظیمات > مدیریت هوش مصنوعی» یک مدل رایگان فعال کنید یا کلید API شخصی خود را وارد نمایید.",
                isError = true
            )
        }

        var lastErrorResponse: AiResponse? = null
        for (model in candidates) {
            val provider = multiConfig.providers.find { it.id == model.providerId } ?: continue
            if (!provider.isEnabled) continue
            if (provider.requiresApiKey && apiKey.isBlank()) continue
            if (model.isLocal && !isDeviceCapableForLocalModel()) continue

            val effectiveSettings = legacySettings.copy(
                aiProvider = provider.providerType,
                aiBaseUrl = provider.baseUrl,
                aiModelName = model.modelName,
                aiFallbackModelName = "",
                aiMaxTokens = model.maxOutputTokens,
                aiOnlineEnabled = if (model.isLocal) true else legacySettings.aiOnlineEnabled
            )

            val response = providerClient.chat(
                userMessage = userMessage,
                history = history,
                memories = memories,
                settings = effectiveSettings,
                apiKey = if (provider.requiresApiKey) apiKey else ""
            )

            if (!response.isError) {
                val now = System.currentTimeMillis()
                val j = PersianCalendarUtils.millisToJalali(now)
                val dayKey = "${j.year}-${j.month}-${j.day}"
                val monthKey = "${j.year}-${j.month}"
                settingsStore.recordAiUsage(tokensUsed = 250, dayKey = dayKey, monthKey = monthKey)
                return response
            } else {
                lastErrorResponse = response
            }
        }

        return lastErrorResponse ?: AiResponse(
            text = "امکان برقراری ارتباط با هیچ‌یک از مدل‌های فعال وجود نداشت. لطفاً اتصال اینترنت یا کلید API خود را بررسی کنید.",
            isError = true
        )
    }

    private fun buildCandidateModels(config: AiMultiModelConfig): List<AiModelConfig> {
        val eligible = config.models.filter { config.canUseModel(it) }
        if (eligible.isEmpty()) return emptyList()

        if (!config.autoRoutingEnabled && config.primaryModelId != null) {
            val primary = eligible.find { it.id == config.primaryModelId }
            val fallbacks = config.fallbackModelIds.mapNotNull { id -> eligible.find { it.id == id } }
            return (listOfNotNull(primary) + fallbacks).distinctBy { it.id }
        }

        val primary = eligible.find { it.id == config.primaryModelId }
        val fallbacks = config.fallbackModelIds.mapNotNull { id -> eligible.find { it.id == id } }
        val remaining = eligible.sortedBy { it.priorityOrder }
        return (listOfNotNull(primary) + fallbacks + remaining).distinctBy { it.id }
    }
}
