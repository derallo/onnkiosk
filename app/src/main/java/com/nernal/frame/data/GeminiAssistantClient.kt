package com.nernal.frame.data

import android.util.Log
import com.nernal.frame.BuildConfig
import com.nernal.frame.hardware.VoiceAndAudioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class ConversationTurn(
    val role: String, // "user" or "model"
    val text: String,
)

data class GeminiAssistantResponse(
    val spokenText: String,
    val displayCard: QuickAnswerCardData? = null,
    val actions: List<VoiceAction> = emptyList(),
    val shouldEndConversation: Boolean = false,
)

/**
 * Tier 2 ("Super Nernal") Gemini API client using gemini-2.5-flash with function calling,
 * local/Pi Memory Book awareness, and optional camera JPEG snapshot vision.
 */
class GeminiAssistantClient(
    private val httpClient: OkHttpClient,
) {
    suspend fun ask(
        apiKey: String,
        userText: String,
        ctx: VoiceContextSnapshot,
        memoryEntries: List<MemoryEntry>,
        history: List<ConversationTurn>,
        imageBase64Jpeg: String? = null,
    ): Result<GeminiAssistantResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Gemini API key is missing." }

            val systemText = buildSystemPrompt(ctx, memoryEntries)
            val contents = JSONArray()

            // Include recent turns for natural multi-turn follow-ups
            for (turn in history.takeLast(6)) {
                contents.put(
                    JSONObject().apply {
                        put("role", turn.role)
                        put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                    }
                )
            }

            val currentParts = JSONArray().apply {
                put(JSONObject().put("text", userText))
                if (!imageBase64Jpeg.isNullOrBlank()) {
                    put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", imageBase64Jpeg)
                            }
                        )
                    )
                }
            }
            contents.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", currentParts)
                }
            )

            val collectedActions = mutableListOf<VoiceAction>()
            var shouldEnd = false
            var finalText = ""

            for (round in 0 until 2) {
                val payload = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText))))
                    put("contents", contents)
                    put("tools", JSONArray().put(JSONObject().put("functionDeclarations", buildToolDeclarations())))
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.6)
                            put("maxOutputTokens", 450)
                        }
                    )
                }

                val req = Request.Builder()
                    .url(ENDPOINT_URL)
                    .header("x-goog-api-key", key)
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val respJson = httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val err = NernalRepository.extractGoogleApiError(body, "Gemini API HTTP ${resp.code}")
                        error(err)
                    }
                    JSONObject(body)
                }

                val candidates = respJson.optJSONArray("candidates")
                val modelContent = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = modelContent?.optJSONArray("parts") ?: JSONArray()

                val textParts = mutableListOf<String>()
                val functionCalls = mutableListOf<Pair<String, JSONObject>>()

                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i) ?: continue
                    val txt = p.optString("text")
                    if (txt.isNotBlank()) textParts.add(txt)
                    val fc = p.optJSONObject("functionCall")
                    if (fc != null) {
                        val name = fc.optString("name")
                        val args = fc.optJSONObject("args") ?: JSONObject()
                        if (name.isNotBlank()) functionCalls.add(name to args)
                    }
                }

                if (textParts.isNotEmpty()) {
                    finalText = textParts.joinToString(" ").trim()
                }

                if (functionCalls.isEmpty()) break

                contents.put(modelContent!!)
                val responseParts = JSONArray()
                var needsSecondRound = false

                for ((name, args) in functionCalls) {
                    val outcome = executeLocalToolCall(name, args, ctx, memoryEntries)
                    collectedActions.addAll(outcome.actions)
                    if (outcome.shouldEnd) shouldEnd = true
                    if (outcome.needsModelFollowup) needsSecondRound = true

                    responseParts.put(
                        JSONObject().put(
                            "functionResponse",
                            JSONObject().apply {
                                put("name", name)
                                put("response", outcome.toolResult)
                            }
                        )
                    )
                }

                contents.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", responseParts)
                    }
                )

                if (!needsSecondRound && finalText.isNotBlank()) break
            }

            if (finalText.isBlank()) {
                finalText = when {
                    shouldEnd -> "Goodbye! Talk to you soon."
                    collectedActions.isNotEmpty() -> "Done!"
                    else -> "I'm ready whenever you need me."
                }
            }

            val cleanSpoken = VoiceAndAudioController.sanitizeForSpeech(finalText)
            val card = if (cleanSpoken.length > 70 || collectedActions.isEmpty()) {
                QuickAnswerCardData(question = userText, answer = cleanSpoken)
            } else {
                null
            }

            GeminiAssistantResponse(
                spokenText = cleanSpoken,
                displayCard = card,
                actions = collectedActions,
                shouldEndConversation = shouldEnd || OnDeviceVoiceEngine.isClosingPhrase(userText),
            )
        }.onFailure { err ->
            Log.w("NernalGemini", "ask failed: ${err.message}")
        }
    }

    private data class ToolOutcome(
        val toolResult: JSONObject,
        val actions: List<VoiceAction> = emptyList(),
        val shouldEnd: Boolean = false,
        val needsModelFollowup: Boolean = true,
    )

    private fun executeLocalToolCall(
        name: String,
        args: JSONObject,
        ctx: VoiceContextSnapshot,
        memoryEntries: List<MemoryEntry>,
    ): ToolOutcome {
        return when (name) {
            "add_calendar_or_task" -> {
                val text = args.optString("text").trim()
                val catStr = args.optString("category", "task").lowercase(Locale.US)
                val cat = if (catStr == "event" || catStr == "calendar") ReminderCategory.EVENT else ReminderCategory.TASK
                val dateKey = args.optString("date").takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: ctx.todayKey
                val time = args.optString("time").takeIf { it.isNotBlank() && it != "null" }
                if (text.isBlank()) {
                    ToolOutcome(JSONObject().put("error", "Missing text"))
                } else {
                    ToolOutcome(
                        toolResult = JSONObject().put("result", "Added ${cat.label.lowercase()}: $text on $dateKey"),
                        actions = listOf(VoiceAction.AddReminder(text, cat, dateKey, time)),
                    )
                }
            }

            "complete_or_delete_item" -> {
                val query = args.optString("query").trim()
                val action = args.optString("action", "complete").lowercase(Locale.US)
                if (query.isBlank()) {
                    ToolOutcome(JSONObject().put("error", "Missing query"))
                } else if (action == "delete") {
                    ToolOutcome(
                        toolResult = JSONObject().put("result", "Requested delete for items matching $query"),
                        actions = listOf(VoiceAction.DeleteReminder(query)),
                    )
                } else {
                    ToolOutcome(
                        toolResult = JSONObject().put("result", "Marked matching task complete: $query"),
                        actions = listOf(VoiceAction.CompleteReminder(query)),
                    )
                }
            }

            "control_dashboard" -> {
                val action = args.optString("action").trim()
                ToolOutcome(
                    toolResult = JSONObject().put("result", "Executed $action"),
                    actions = listOf(VoiceAction.DashboardCommand(action)),
                )
            }

            "save_memory_entry" -> {
                val notebook = args.optString("notebook", "notes").ifBlank { "notes" }
                val title = args.optString("title").trim()
                val summary = args.optString("summary").trim()
                val content = args.optString("content").trim()
                if (title.isBlank() || content.isBlank()) {
                    ToolOutcome(JSONObject().put("error", "Title and content are required"))
                } else {
                    ToolOutcome(
                        toolResult = JSONObject().put("result", "Saved \"$title\" to $notebook."),
                        actions = listOf(VoiceAction.SaveMemory(notebook, title, summary.ifBlank { content.take(80) }, content)),
                    )
                }
            }

            "search_memory_entries" -> {
                val query = args.optString("query").trim().lowercase(Locale.US)
                val hits = if (query.isBlank()) {
                    memoryEntries.take(8)
                } else {
                    memoryEntries.filter {
                        it.title.lowercase(Locale.US).contains(query) ||
                            it.content.lowercase(Locale.US).contains(query) ||
                            it.notebook.lowercase(Locale.US).contains(query)
                    }.take(6)
                }
                val formatted = if (hits.isEmpty()) {
                    "No matching memories found."
                } else {
                    hits.joinToString("\n\n") { "[${it.notebook}] ${it.title}: ${it.content}" }
                }
                ToolOutcome(
                    toolResult = JSONObject().put("result", formatted),
                    needsModelFollowup = true,
                )
            }

            "end_conversation" -> ToolOutcome(
                toolResult = JSONObject().put("result", "Closing conversation."),
                actions = listOf(VoiceAction.EndConversation),
                shouldEnd = true,
                needsModelFollowup = true,
            )

            else -> ToolOutcome(JSONObject().put("error", "Unknown tool $name"))
        }
    }

    private fun buildSystemPrompt(
        ctx: VoiceContextSnapshot,
        memoryEntries: List<MemoryEntry>,
    ): String {
        val weatherSummary = ctx.weather?.let { w ->
            val today = w.daily.firstOrNull()
            "${w.currentTempF}°F, ${w.description}" +
                (today?.let { " (High ${it.maxTempF}°F, Low ${it.minTempF}°F, Rain ${it.precipChance}%)" } ?: "")
        } ?: "Loading"

        val upcomingItems = ctx.reminders
            .filter { !it.completed && (it.dateKey == null || it.dateKey >= ctx.todayKey) }
            .take(15)
            .joinToString("\n") { r ->
                "- [${r.category.label}] ${r.text}" +
                    (r.dateKey?.let { " ($it${r.time?.let { t -> " $t" } ?: ""})" } ?: "")
            }.ifBlank { "(none)" }

        val memIndex = memoryEntries.take(25).joinToString("\n") { m ->
            "- [${m.notebook}] ${m.title}: ${m.summary.ifBlank { m.content.take(60) }}"
        }.ifBlank { "(none yet)" }

        return """
You are ${BuildConfig.APP_DISPLAY_NAME}, a warm, polite, clear household ambient assistant running on a countertop smart display in ${ctx.locationName}.
Speak in plain conversational English suitable for reading aloud (1 to 3 concise sentences). NEVER use markdown, bullet symbols, asterisks, hashtags, or URLs.

Current Time: ${ctx.clockTime} ${ctx.clockAmPm} on ${ctx.clockDate} (${ctx.todayKey})
Current Weather: $weatherSummary
Display State: fullscreenPhotos=${ctx.isFullscreen}, calendarPrimary=${ctx.isCalendarPrimary}, radarOpen=${ctx.showRadarModal}

Upcoming Events & Active Tasks:
$upcomingItems

Saved Memory Book Index (recipes, family notes, contacts, health/home notes):
$memIndex

Rules:
- Use search_memory_entries before answering detailed questions about a saved recipe or note.
- Proactively call save_memory_entry when the user shares a recipe, family fact, contact, or household note worth remembering.
- Use add_calendar_or_task / complete_or_delete_item / control_dashboard when asked to add/check off items or control the screen.
- If the user says goodbye or indicates they are done, call end_conversation and give a brief 1-sentence parting reply.
- If an image from the tablet camera is attached, describe or read what is visible naturally and helpfully.
        """.trimIndent()
    }

    private fun buildToolDeclarations(): JSONArray = JSONArray().apply {
        put(
            JSONObject().apply {
                put("name", "add_calendar_or_task")
                put("description", "Add a new task/reminder or calendar event.")
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("text", JSONObject().put("type", "STRING").put("description", "Title of task or event"))
                                put("category", JSONObject().put("type", "STRING").put("description", "task or event"))
                                put("date", JSONObject().put("type", "STRING").put("description", "Optional YYYY-MM-DD date"))
                                put("time", JSONObject().put("type", "STRING").put("description", "Optional time like 2:00 PM"))
                            }
                        )
                        put("required", JSONArray().put("text").put("category"))
                    }
                )
            }
        )
        put(
            JSONObject().apply {
                put("name", "complete_or_delete_item")
                put("description", "Mark a matching task complete or delete a matching task/event.")
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("query", JSONObject().put("type", "STRING").put("description", "Keywords matching item text"))
                                put("action", JSONObject().put("type", "STRING").put("description", "complete or delete"))
                            }
                        )
                        put("required", JSONArray().put("query").put("action"))
                    }
                )
            }
        )
        put(
            JSONObject().apply {
                put("name", "control_dashboard")
                put("description", "Control the dashboard UI: open_radar, close_radar, next_photo, previous_photo, star_photo, fullscreen_photos, exit_fullscreen, show_calendar_primary, show_photos, dim_screen.")
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("action", JSONObject().put("type", "STRING").put("description", "Action name"))
                            }
                        )
                        put("required", JSONArray().put("action"))
                    }
                )
            }
        )
        put(
            JSONObject().apply {
                put("name", "save_memory_entry")
                put("description", "Save a recipe, family note, contact, health/home note into the Memory Book.")
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("notebook", JSONObject().put("type", "STRING").put("description", "recipes, family, health, home, contacts, or notes"))
                                put("title", JSONObject().put("type", "STRING"))
                                put("summary", JSONObject().put("type", "STRING"))
                                put("content", JSONObject().put("type", "STRING"))
                            }
                        )
                        put("required", JSONArray().put("notebook").put("title").put("content"))
                    }
                )
            }
        )
        put(
            JSONObject().apply {
                put("name", "search_memory_entries")
                put("description", "Read full Memory Book entries matching a keyword query.")
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("query", JSONObject().put("type", "STRING"))
                            }
                        )
                    }
                )
            }
        )
        put(
            JSONObject().apply {
                put("name", "end_conversation")
                put("description", "Close the active voice conversation when the user says goodbye or is finished.")
                put("parameters", JSONObject().apply { put("type", "OBJECT").put("properties", JSONObject()) })
            }
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val ENDPOINT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        private val VISION_HINT_REGEX = Regex(
            """\b(?:what am i holding|what is this|what'?s this|look at|see this|read this|scan this|how do i look|in my hand|in front of|camera|take a look|what does this say)\b""",
            RegexOption.IGNORE_CASE,
        )

        fun looksLikeVisionQuestion(text: String): Boolean =
            VISION_HINT_REGEX.containsMatchIn(text)
    }
}
