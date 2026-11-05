package com.example.ai

import com.example.data.local.AiProviderType
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.YadetSettings
import com.example.util.PersianCalendarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val text: String,
    val executedTools: List<String> = emptyList(),
    val isError: Boolean = false
)

class AiProviderClient(private val toolExecutor: ToolExecutor) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isModelConfigured(settings: YadetSettings, apiKey: String): Boolean {
        if (!settings.aiOnlineEnabled) return false
        if (settings.aiProvider == AiProviderType.NONE) return false
        if (settings.aiProvider != AiProviderType.CUSTOM && apiKey.isBlank()) return false
        return settings.aiBaseUrl.isNotBlank() && settings.aiModelName.isNotBlank()
    }

    suspend fun testConnection(settings: YadetSettings, apiKey: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (settings.aiProvider == AiProviderType.NONE) {
                return@withContext Result.failure(IllegalStateException("هیچ ارائه‌دهنده‌ای انتخاب نشده است."))
            }
            if (settings.aiProvider != AiProviderType.CUSTOM && apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("کلید API وارد نشده است."))
            }
            try {
                val baseUrl = settings.aiBaseUrl.trimEnd('/')
                val req = if (settings.aiProvider == AiProviderType.GEMINI_REST) {
                    Request.Builder()
                        .url("$baseUrl/models?key=$apiKey")
                        .get()
                        .build()
                } else {
                    val builder = Request.Builder().url("$baseUrl/models").get()
                    if (apiKey.isNotBlank()) {
                        builder.header("Authorization", "Bearer $apiKey")
                    }
                    builder.build()
                }
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    Result.success("اتصال به ${settings.aiProvider.persianLabel} با موفقیت برقرار شد.")
                } else {
                    Result.failure(IllegalStateException("خطای سرور (${resp.code}): ${resp.message}"))
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException("خطا در اتصال: ${e.localizedMessage}"))
            }
        }

    suspend fun chat(
        userMessage: String,
        history: List<ChatMessageEntity>,
        memories: List<MemoryItemEntity>,
        settings: YadetSettings,
        apiKey: String
    ): AiResponse = withContext(Dispatchers.IO) {
        if (!isModelConfigured(settings, apiKey)) {
            return@withContext AiResponse(
                text = "در حال حاضر هیچ مدل هوش مصنوعی فعالی تنظیم نشده است. برای مکالمه هوشمند، از بخش «تنظیمات > مدل هوش مصنوعی» ارائه‌دهنده و کلید API شخصی خود را وارد کنید. همچنین می‌توانید همین حالا یادآورها، پروژه‌ها و یادداشت‌های خود را به صورت آفلاین مدیریت کنید.",
                isError = true
            )
        }

        val systemPrompt = buildSystemPrompt(memories)
        val primaryResult = callProvider(
            model = settings.aiModelName,
            systemPrompt = systemPrompt,
            history = history,
            userMessage = userMessage,
            settings = settings,
            apiKey = apiKey
        )

        if (!primaryResult.isError || settings.aiFallbackModelName.isBlank()) {
            return@withContext primaryResult
        }

        // Try fallback model if configured
        callProvider(
            model = settings.aiFallbackModelName,
            systemPrompt = systemPrompt,
            history = history,
            userMessage = userMessage,
            settings = settings,
            apiKey = apiKey
        )
    }

    private suspend fun callProvider(
        model: String,
        systemPrompt: String,
        history: List<ChatMessageEntity>,
        userMessage: String,
        settings: YadetSettings,
        apiKey: String
    ): AiResponse {
        return try {
            if (settings.aiProvider == AiProviderType.GEMINI_REST) {
                callGeminiRest(model, systemPrompt, history, userMessage, settings, apiKey)
            } else {
                callOpenAiCompatible(model, systemPrompt, history, userMessage, settings, apiKey)
            }
        } catch (e: Exception) {
            AiResponse(
                text = "خطا در ارتباط با مدل هوش مصنوعی: ${e.localizedMessage}",
                isError = true
            )
        }
    }

    private suspend fun callOpenAiCompatible(
        model: String,
        systemPrompt: String,
        history: List<ChatMessageEntity>,
        userMessage: String,
        settings: YadetSettings,
        apiKey: String
    ): AiResponse {
        val baseUrl = settings.aiBaseUrl.trimEnd('/')
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            history.takeLast(12).forEach { msg ->
                val role = if (msg.role == "USER") "user" else "assistant"
                put(JSONObject().put("role", role).put("content", msg.content))
            }
            put(JSONObject().put("role", "user").put("content", userMessage))
        }

        val executedTools = mutableListOf<String>()
        var loops = 0
        while (loops < 4) {
            loops++
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("max_tokens", settings.aiMaxTokens)
                put("tools", buildOpenAiToolsSchema())
            }
            val reqBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
            if (apiKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $apiKey")
            }

            val resp = client.newCall(reqBuilder.build()).execute()
            val respStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return AiResponse("خطای سرویس هوش مصنوعی (${resp.code})", executedTools, isError = true)
            }

            val respJson = JSONObject(respStr)
            val choice = respJson.getJSONArray("choices").getJSONObject(0)
            val messageObj = choice.getJSONObject("message")
            val toolCalls = messageObj.optJSONArray("tool_calls")

            if (toolCalls == null || toolCalls.length() == 0) {
                val content = messageObj.optString("content", "").trim()
                return AiResponse(content.ifBlank { "پاسخی دریافت نشد." }, executedTools)
            }

            messages.put(messageObj)
            for (i in 0 until toolCalls.length()) {
                val tc = toolCalls.getJSONObject(i)
                val tcId = tc.getString("id")
                val fn = tc.getJSONObject("function")
                val fnName = fn.getString("name")
                val fnArgs = fn.optString("arguments", "{}")
                val execRes = toolExecutor.executeTool(fnName, fnArgs)
                executedTools.add(execRes.humanSummary)

                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", tcId)
                        .put("content", execRes.resultJson)
                )
            }
        }
        return AiResponse("عملیات انجام شد.", executedTools)
    }

    private suspend fun callGeminiRest(
        model: String,
        systemPrompt: String,
        history: List<ChatMessageEntity>,
        userMessage: String,
        settings: YadetSettings,
        apiKey: String
    ): AiResponse {
        val baseUrl = settings.aiBaseUrl.trimEnd('/')
        val contents = JSONArray().apply {
            history.takeLast(12).forEach { msg ->
                val role = if (msg.role == "USER") "user" else "model"
                put(
                    JSONObject()
                        .put("role", role)
                        .put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                )
            }
            put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            )
        }

        val executedTools = mutableListOf<String>()
        var loops = 0
        while (loops < 4) {
            loops++
            val bodyJson = JSONObject().apply {
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                put("contents", contents)
                put("generationConfig", JSONObject().put("maxOutputTokens", settings.aiMaxTokens))
                put("tools", JSONArray().put(JSONObject().put("functionDeclarations", buildGeminiToolsSchema())))
            }

            val req = Request.Builder()
                .url("$baseUrl/models/$model:generateContent?key=$apiKey")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val resp = client.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return AiResponse("خطای سرویس Gemini (${resp.code})", executedTools, isError = true)
            }

            val respJson = JSONObject(respStr)
            val candidate = respJson.getJSONArray("candidates").getJSONObject(0)
            val contentObj = candidate.getJSONObject("content")
            val parts = contentObj.getJSONArray("parts")

            var functionCallPart: JSONObject? = null
            val textBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (p.has("functionCall")) {
                    functionCallPart = p.getJSONObject("functionCall")
                } else if (p.has("text")) {
                    textBuilder.append(p.getString("text"))
                }
            }

            if (functionCallPart == null) {
                return AiResponse(textBuilder.toString().trim().ifBlank { "پاسخی دریافت نشد." }, executedTools)
            }

            contents.put(contentObj)
            val fnName = functionCallPart.getString("name")
            val fnArgs = functionCallPart.optJSONObject("args")?.toString() ?: "{}"
            val execRes = toolExecutor.executeTool(fnName, fnArgs)
            executedTools.add(execRes.humanSummary)

            contents.put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "functionResponse",
                                JSONObject()
                                    .put("name", fnName)
                                    .put("response", JSONObject().put("result", execRes.resultJson))
                            )
                        )
                    )
            )
        }
        return AiResponse("عملیات انجام شد.", executedTools)
    }

    private fun buildSystemPrompt(memories: List<MemoryItemEntity>): String {
        val now = System.currentTimeMillis()
        val jalali = PersianCalendarUtils.millisToJalali(now).formatFull()
        val weekday = PersianCalendarUtils.getPersianDayOfWeekName(now)
        val time = PersianCalendarUtils.formatTimeOnly(now, true)
        val memText = if (memories.isEmpty()) {
            "ندارد"
        } else {
            memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
        }

        return """
تو «یادت» (Yadet) هستی؛ یک دستیار هوش مصنوعی فارسی، دقیق، منطقی، محترمانه و همراه هوشمند زندگی.
زمان فعلی: $weekday $jalali، ساعت $time

قوانین بسیار مهم:
۱. هر پیام کاربر فرمان ایجاد یادآور نیست! اگر کاربر مشورت می‌خواهد (مثلاً «به نظرت ورزش ساعت ۱۲ شب خوبه؟») یا صرفاً اطلاعاتی بیان می‌کند (مثلاً «من معمولاً ساعت ۸ صبح بیدار میشم» یا «فردا باید برم بانک» بدون درخواست یادآور)، فقط پاسخ مشاوره‌ای یا گفتگوی طبیعی بده و هیچ یادآوری نساز.
۲. فقط زمانی ابزار create_reminder را صدا بزن که کاربر صراحتاً درخواست یادآور یا برنامه‌ریزی داشته باشد.
۳. اگر عنوان یا زمان ضروری مشخص نیست، حدس نزن؛ یک سؤال کوتاه و محترمانه بپرس (مثلاً «برای چه کاری یادآوری تنظیم کنم؟»).
۴. برای تغییر تنظیمات اعلان یا برنامه از ابزارهای change_notification_settings و update_app_settings استفاده کن.
۵. هرگز قبل از اجرای واقعی ابزار، پیام موفقیت نده.

حافظه بلندمدت کاربر:
$memText
        """.trimIndent()
    }

    private fun buildOpenAiToolsSchema(): JSONArray {
        val tools = listOf(
            toolDef("create_reminder", "ایجاد یادآور جدید", mapOf(
                "title" to "عنوان یادآور (الزامی)",
                "datetime" to "تاریخ و ساعت به فارسی یا شمسی (مثلاً فردا ساعت ۸ صبح یا 1405/08/15 09:00)",
                "description" to "توضیحات تکمیلی",
                "priority" to "LOW, NORMAL, HIGH, URGENT",
                "recurrence" to "NONE, DAILY, WEEKLY, MONTHLY, YEARLY, EVEN_DAYS, ODD_DAYS, SATURDAY_TO_WEDNESDAY"
            ), listOf("title", "datetime")),
            toolDef("list_reminders", "نمایش فهرست یادآورها", mapOf(
                "filter" to "ACTIVE, TODAY, OVERDUE, COMPLETED"
            )),
            toolDef("search_reminders", "جستجو در یادآورها", mapOf(
                "query" to "عبارت جستجو"
            ), listOf("query")),
            toolDef("update_reminder", "ویرایش یادآور", mapOf(
                "id" to "شناسه عددی یادآور",
                "title" to "عنوان جدید",
                "datetime" to "زمان جدید"
            ), listOf("id")),
            toolDef("delete_reminder", "انتقال یادآور به سطل زباله", mapOf(
                "id" to "شناسه عددی یادآور"
            ), listOf("id")),
            toolDef("complete_reminder", "علامت‌گذاری یادآور به عنوان انجام‌شده", mapOf(
                "id" to "شناسه عددی یادآور"
            ), listOf("id")),
            toolDef("change_notification_settings", "تغییر تنظیمات اعلان یادآورها", mapOf(
                "lead_minutes" to "چند دقیقه زودتر اطلاع داده شود (عدد)",
                "snooze_minutes" to "مدت تعویق پیش‌فرض (دقیقه)"
            )),
            toolDef("update_app_settings", "تغییر تنظیمات ظاهری برنامه", mapOf(
                "theme" to "DARK, LIGHT, SYSTEM",
                "calendar" to "JALALI, GREGORIAN"
            )),
            toolDef("get_current_datetime", "دریافت تاریخ شمسی و ساعت فعلی", emptyMap()),
            toolDef("save_memory", "ذخیره ترجیح یا هدف مهم کاربر در حافظه", mapOf(
                "key" to "عنوان کوتاه",
                "value" to "مقدار"
            ), listOf("key", "value")),
            toolDef("web_search", "جستجوی اینترنتی در صورت نیاز به اطلاعات به‌روز", mapOf(
                "query" to "عبارت جستجو"
            ), listOf("query"))
        )
        return JSONArray(tools)
    }

    private fun buildGeminiToolsSchema(): JSONArray {
        val openAi = buildOpenAiToolsSchema()
        val arr = JSONArray()
        for (i in 0 until openAi.length()) {
            arr.put(openAi.getJSONObject(i).getJSONObject("function"))
        }
        return arr
    }

    private fun toolDef(
        name: String,
        description: String,
        props: Map<String, String>,
        required: List<String> = emptyList()
    ): JSONObject {
        val propsObj = JSONObject()
        props.forEach { (k, v) ->
            propsObj.put(k, JSONObject().put("type", "string").put("description", v))
        }
        val params = JSONObject()
            .put("type", "object")
            .put("properties", propsObj)
        if (required.isNotEmpty()) {
            params.put("required", JSONArray(required))
        }
        return JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("parameters", params)
            )
    }
}
