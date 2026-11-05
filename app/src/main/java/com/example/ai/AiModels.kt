package com.example.ai

import com.example.data.local.AiProviderType
import org.json.JSONArray
import org.json.JSONObject

enum class AiConnectionStatus(val persianLabel: String) {
    UNTESTED("آزمایش‌نشده"),
    CONNECTED("متصل"),
    FAILED("خطا در اتصال"),
    QUOTA_EXCEEDED("اتمام سهمیه"),
    OFFLINE("آفلاین")
}

enum class PricingVerificationStatus(val persianLabel: String) {
    VERIFIED_FREE("رایگان (تأییدشده)"),
    UNVERIFIED("وضعیت هزینه نامشخص"),
    PAID("پولی (نیازمند تأیید کاربر)")
}

enum class AiModelRole(val persianLabel: String) {
    PRIMARY("مدل اصلی"),
    FALLBACK("مدل پشتیبان"),
    SECONDARY("مدل کمکی")
}

data class AiProviderInfo(
    val id: String,
    val name: String,
    val persianName: String,
    val providerType: AiProviderType,
    val baseUrl: String,
    val isLocal: Boolean = false,
    val requiresApiKey: Boolean = true,
    val isEnabled: Boolean = true,
    val connectionStatus: AiConnectionStatus = AiConnectionStatus.UNTESTED,
    val lastTestMessage: String = "",
    val lastTestedAtMillis: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("persianName", persianName)
        put("providerType", providerType.name)
        put("baseUrl", baseUrl)
        put("isLocal", isLocal)
        put("requiresApiKey", requiresApiKey)
        put("isEnabled", isEnabled)
        put("connectionStatus", connectionStatus.name)
        put("lastTestMessage", lastTestMessage)
        put("lastTestedAtMillis", lastTestedAtMillis)
    }

    companion object {
        fun fromJson(obj: JSONObject): AiProviderInfo {
            val type = runCatching {
                AiProviderType.valueOf(obj.optString("providerType", AiProviderType.NONE.name))
            }.getOrDefault(AiProviderType.NONE)
            val status = runCatching {
                AiConnectionStatus.valueOf(obj.optString("connectionStatus", AiConnectionStatus.UNTESTED.name))
            }.getOrDefault(AiConnectionStatus.UNTESTED)
            return AiProviderInfo(
                id = obj.getString("id"),
                name = obj.optString("name", ""),
                persianName = obj.optString("persianName", ""),
                providerType = type,
                baseUrl = obj.optString("baseUrl", ""),
                isLocal = obj.optBoolean("isLocal", false),
                requiresApiKey = obj.optBoolean("requiresApiKey", true),
                isEnabled = obj.optBoolean("isEnabled", true),
                connectionStatus = status,
                lastTestMessage = obj.optString("lastTestMessage", ""),
                lastTestedAtMillis = obj.optLong("lastTestedAtMillis", 0L)
            )
        }
    }
}

data class AiModelConfig(
    val id: String,
    val providerId: String,
    val modelName: String,
    val displayName: String,
    val isLocal: Boolean = false,
    val isFree: Boolean = true,
    val pricingStatus: PricingVerificationStatus = PricingVerificationStatus.VERIFIED_FREE,
    val supportsToolCalling: Boolean = false,
    val supportsPersian: Boolean = true,
    val supportsStructuredOutput: Boolean = false,
    val isEnabled: Boolean = true,
    val priorityOrder: Int = 0,
    val role: AiModelRole = AiModelRole.SECONDARY,
    val maxContextTokens: Int = 8192,
    val maxOutputTokens: Int = 1024,
    val dailyRequestLimit: Int? = null,
    val knownLimitations: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("providerId", providerId)
        put("modelName", modelName)
        put("displayName", displayName)
        put("isLocal", isLocal)
        put("isFree", isFree)
        put("pricingStatus", pricingStatus.name)
        put("supportsToolCalling", supportsToolCalling)
        put("supportsPersian", supportsPersian)
        put("supportsStructuredOutput", supportsStructuredOutput)
        put("isEnabled", isEnabled)
        put("priorityOrder", priorityOrder)
        put("role", role.name)
        put("maxContextTokens", maxContextTokens)
        put("maxOutputTokens", maxOutputTokens)
        if (dailyRequestLimit != null) put("dailyRequestLimit", dailyRequestLimit)
        put("knownLimitations", knownLimitations)
    }

    companion object {
        fun fromJson(obj: JSONObject): AiModelConfig {
            val pricing = runCatching {
                PricingVerificationStatus.valueOf(
                    obj.optString("pricingStatus", PricingVerificationStatus.UNVERIFIED.name)
                )
            }.getOrDefault(PricingVerificationStatus.UNVERIFIED)
            val role = runCatching {
                AiModelRole.valueOf(obj.optString("role", AiModelRole.SECONDARY.name))
            }.getOrDefault(AiModelRole.SECONDARY)
            return AiModelConfig(
                id = obj.getString("id"),
                providerId = obj.getString("providerId"),
                modelName = obj.getString("modelName"),
                displayName = obj.optString("displayName", obj.getString("modelName")),
                isLocal = obj.optBoolean("isLocal", false),
                isFree = obj.optBoolean("isFree", true),
                pricingStatus = pricing,
                supportsToolCalling = obj.optBoolean("supportsToolCalling", false),
                supportsPersian = obj.optBoolean("supportsPersian", true),
                supportsStructuredOutput = obj.optBoolean("supportsStructuredOutput", false),
                isEnabled = obj.optBoolean("isEnabled", true),
                priorityOrder = obj.optInt("priorityOrder", 0),
                role = role,
                maxContextTokens = obj.optInt("maxContextTokens", 8192),
                maxOutputTokens = obj.optInt("maxOutputTokens", 1024),
                dailyRequestLimit = if (obj.has("dailyRequestLimit") && !obj.isNull("dailyRequestLimit")) {
                    obj.getInt("dailyRequestLimit")
                } else null,
                knownLimitations = obj.optString("knownLimitations", "")
            )
        }
    }
}

data class AiUsageStats(
    val dailyRequestCount: Int = 0,
    val dailyTokensUsed: Int = 0,
    val monthlyRequestCount: Int = 0,
    val lastResetDayKey: String = "",
    val lastResetMonthKey: String = "",
    val dailyRequestLimit: Int = 100,
    val monthlyRequestLimit: Int = 2000
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("dailyRequestCount", dailyRequestCount)
        put("dailyTokensUsed", dailyTokensUsed)
        put("monthlyRequestCount", monthlyRequestCount)
        put("lastResetDayKey", lastResetDayKey)
        put("lastResetMonthKey", lastResetMonthKey)
        put("dailyRequestLimit", dailyRequestLimit)
        put("monthlyRequestLimit", monthlyRequestLimit)
    }

    companion object {
        fun fromJson(obj: JSONObject): AiUsageStats = AiUsageStats(
            dailyRequestCount = obj.optInt("dailyRequestCount", 0),
            dailyTokensUsed = obj.optInt("dailyTokensUsed", 0),
            monthlyRequestCount = obj.optInt("monthlyRequestCount", 0),
            lastResetDayKey = obj.optString("lastResetDayKey", ""),
            lastResetMonthKey = obj.optString("lastResetMonthKey", ""),
            dailyRequestLimit = obj.optInt("dailyRequestLimit", 100),
            monthlyRequestLimit = obj.optInt("monthlyRequestLimit", 2000)
        )
    }
}

data class AiMultiModelConfig(
    val autoRoutingEnabled: Boolean = true,
    val allowPaidModels: Boolean = false,
    val primaryModelId: String? = null,
    val fallbackModelIds: List<String> = emptyList(),
    val providers: List<AiProviderInfo> = DefaultAiCatalog.defaultProviders(),
    val models: List<AiModelConfig> = DefaultAiCatalog.defaultModels(),
    val usageStats: AiUsageStats = AiUsageStats()
) {
    fun canUseModel(model: AiModelConfig): Boolean {
        if (!model.isEnabled) return false
        if (model.pricingStatus != PricingVerificationStatus.VERIFIED_FREE && !allowPaidModels) return false
        if (!model.isFree && !allowPaidModels) return false
        return true
    }

    fun toJsonString(): String = JSONObject().apply {
        put("autoRoutingEnabled", autoRoutingEnabled)
        put("allowPaidModels", allowPaidModels)
        if (primaryModelId != null) put("primaryModelId", primaryModelId)
        put("fallbackModelIds", JSONArray(fallbackModelIds))
        put("providers", JSONArray().apply { providers.forEach { put(it.toJson()) } })
        put("models", JSONArray().apply { models.forEach { put(it.toJson()) } })
        put("usageStats", usageStats.toJson())
    }.toString()

    companion object {
        fun fromJsonString(raw: String): AiMultiModelConfig? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val fallbacks = mutableListOf<String>()
                val fbArr = obj.optJSONArray("fallbackModelIds") ?: JSONArray()
                for (i in 0 until fbArr.length()) {
                    fallbacks.add(fbArr.getString(i))
                }
                val providers = mutableListOf<AiProviderInfo>()
                val pArr = obj.optJSONArray("providers") ?: JSONArray()
                for (i in 0 until pArr.length()) {
                    providers.add(AiProviderInfo.fromJson(pArr.getJSONObject(i)))
                }
                val models = mutableListOf<AiModelConfig>()
                val mArr = obj.optJSONArray("models") ?: JSONArray()
                for (i in 0 until mArr.length()) {
                    models.add(AiModelConfig.fromJson(mArr.getJSONObject(i)))
                }
                val stats = obj.optJSONObject("usageStats")?.let { AiUsageStats.fromJson(it) }
                    ?: AiUsageStats()

                AiMultiModelConfig(
                    autoRoutingEnabled = obj.optBoolean("autoRoutingEnabled", true),
                    allowPaidModels = obj.optBoolean("allowPaidModels", false),
                    primaryModelId = if (obj.has("primaryModelId") && !obj.isNull("primaryModelId")) {
                        obj.getString("primaryModelId")
                    } else null,
                    fallbackModelIds = fallbacks,
                    providers = if (providers.isEmpty()) DefaultAiCatalog.defaultProviders() else providers,
                    models = if (models.isEmpty()) DefaultAiCatalog.defaultModels() else models,
                    usageStats = stats
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class PendingToolConfirmation(
    val id: String,
    val toolName: String,
    val argumentsJson: String,
    val humanDescription: String
)

object DefaultAiCatalog {
    fun defaultProviders(): List<AiProviderInfo> = listOf(
        AiProviderInfo(
            id = "provider_gemini_free",
            name = "Google Gemini",
            persianName = "گوگل جمنای (سهمیه رایگان)",
            providerType = AiProviderType.GEMINI_REST,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            isLocal = false,
            requiresApiKey = true,
            isEnabled = true
        ),
        AiProviderInfo(
            id = "provider_openrouter_free",
            name = "OpenRouter",
            persianName = "اوپن‌روتر (مدل‌های رایگان)",
            providerType = AiProviderType.OPENROUTER,
            baseUrl = "https://openrouter.ai/api/v1",
            isLocal = false,
            requiresApiKey = true,
            isEnabled = true
        ),
        AiProviderInfo(
            id = "provider_local_ollama",
            name = "Local / Custom Server",
            persianName = "سرور محلی / اختصاصی (بدون کلید)",
            providerType = AiProviderType.CUSTOM,
            baseUrl = "http://10.0.2.2:11434/v1",
            isLocal = true,
            requiresApiKey = false,
            isEnabled = false
        )
    )

    fun defaultModels(): List<AiModelConfig> = listOf(
        AiModelConfig(
            id = "model_gemini_2_5_flash",
            providerId = "provider_gemini_free",
            modelName = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash (رایگان)",
            isLocal = false,
            isFree = true,
            pricingStatus = PricingVerificationStatus.VERIFIED_FREE,
            supportsToolCalling = true,
            supportsPersian = true,
            supportsStructuredOutput = true,
            isEnabled = true,
            priorityOrder = 1,
            role = AiModelRole.PRIMARY,
            dailyRequestLimit = 250,
            knownLimitations = "دارای محدودیت تعداد درخواست در دقیقه و روز در پلن رایگان"
        ),
        AiModelConfig(
            id = "model_openrouter_free_auto",
            providerId = "provider_openrouter_free",
            modelName = "openrouter/free",
            displayName = "OpenRouter Free Router (رایگان)",
            isLocal = false,
            isFree = true,
            pricingStatus = PricingVerificationStatus.VERIFIED_FREE,
            supportsToolCalling = true,
            supportsPersian = true,
            supportsStructuredOutput = false,
            isEnabled = true,
            priorityOrder = 2,
            role = AiModelRole.FALLBACK,
            dailyRequestLimit = 50,
            knownLimitations = "محدودیت درخواست روزانه و احتمال صف در ساعات شلوغ"
        ),
        AiModelConfig(
            id = "model_local_qwen_2_5",
            providerId = "provider_local_ollama",
            modelName = "qwen2.5:3b",
            displayName = "Qwen 2.5 3B (محلی / آفلاین)",
            isLocal = true,
            isFree = true,
            pricingStatus = PricingVerificationStatus.VERIFIED_FREE,
            supportsToolCalling = false,
            supportsPersian = true,
            supportsStructuredOutput = false,
            isEnabled = false,
            priorityOrder = 3,
            role = AiModelRole.SECONDARY,
            knownLimitations = "فقط برای گفتگوی عمومی آفلاین؛ بدون پشتیبانی از اجرای فرمان‌ها"
        )
    )
}
