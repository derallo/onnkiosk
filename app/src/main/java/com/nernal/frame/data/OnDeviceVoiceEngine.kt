package com.nernal.frame.data

import java.util.Locale

data class VoiceContextSnapshot(
    val clockTime: String,
    val clockAmPm: String,
    val clockDate: String,
    val todayKey: String,
    val locationName: String,
    val weather: WeatherData?,
    val reminders: List<ReminderItem>,
    val isFullscreen: Boolean,
    val isCalendarPrimary: Boolean,
    val showRadarModal: Boolean,
)

sealed interface VoiceAction {
    data class AddReminder(
        val text: String,
        val category: ReminderCategory,
        val dateKey: String? = null,
        val time: String? = null,
    ) : VoiceAction

    data class CompleteReminder(val query: String) : VoiceAction
    data class DeleteReminder(val query: String) : VoiceAction
    data class SaveMemory(
        val notebook: String,
        val title: String,
        val summary: String,
        val content: String,
    ) : VoiceAction

    data class DashboardCommand(val command: String) : VoiceAction
    data object EndConversation : VoiceAction
}

data class VoiceTurnResult(
    val spokenText: String,
    val card: QuickAnswerCardData? = null,
    val actions: List<VoiceAction> = emptyList(),
    val shouldEndConversation: Boolean = false,
)

/**
 * Pure Kotlin on-device voice command parser & responder (Tier 1: zero API key required).
 * Handles time, date, weather, morning briefings, agenda/tasks, adding/completing items, and dashboard controls.
 */
object OnDeviceVoiceEngine {

    private val CLOSING_REGEX = Regex(
        """\b(?:goodbye|bye\s*bye|bye|that'?s\s+(?:all|everything|it)|i'?m\s+done|we'?re\s+done|stop\s+listening|never\s*mind|cancel|close\s+chat)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val ADD_EVENT_REGEX = Regex(
        """^(?:please\s+)?(?:add\s+(?:an?\s+)?(?:calendar\s+)?event|schedule)\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val ADD_TASK_REGEX = Regex(
        """^(?:please\s+)?(?:remind\s+me\s+to|add\s+(?:a\s+)?(?:task|reminder|to-?do)|put)\s+(.+?)(?:\s+(?:to|on)\s+(?:my\s+)?(?:tasks?|to-?do\s+list|shopping\s+list|list))?$""",
        RegexOption.IGNORE_CASE,
    )
    private val ADD_LIST_REGEX = Regex(
        """^(?:please\s+)?add\s+(.+?)\s+(?:to|on)\s+(?:my\s+)?(?:tasks?|to-?do(?:\s+list)?|shopping(?:\s+list)?|reminders?|list)$""",
        RegexOption.IGNORE_CASE,
    )
    private val COMPLETE_TASK_REGEX = Regex(
        """^(?:please\s+)?(?:check\s+off|mark|complete|finish(?:ed)?)\s+(.+?)(?:\s+(?:as\s+)?(?:done|complete|completed))?$""",
        RegexOption.IGNORE_CASE,
    )

    fun isClosingPhrase(transcript: String): Boolean {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty() || trimmed.endsWith("?")) return false
        return CLOSING_REGEX.containsMatchIn(trimmed)
    }

    fun handleCommand(rawTranscript: String, ctx: VoiceContextSnapshot): VoiceTurnResult {
        val text = rawTranscript.trim()
        val lower = text.lowercase(Locale.US)

        if (isClosingPhrase(text)) {
            return VoiceTurnResult(
                spokenText = "Alright, talk to you later.",
                actions = listOf(VoiceAction.EndConversation),
                shouldEndConversation = true,
            )
        }

        // Dashboard controls
        when {
            lower.contains("open radar") || lower.contains("show radar") || lower.contains("weather radar") ->
                return VoiceTurnResult(
                    spokenText = "Opening live Doppler radar.",
                    actions = listOf(VoiceAction.DashboardCommand("open_radar")),
                )

            lower.contains("close radar") || lower.contains("hide radar") ->
                return VoiceTurnResult(
                    spokenText = "Closing radar.",
                    actions = listOf(VoiceAction.DashboardCommand("close_radar")),
                )

            lower.contains("next photo") || lower.contains("next picture") || lower.contains("skip photo") ->
                return VoiceTurnResult(
                    spokenText = "Showing the next photo.",
                    actions = listOf(VoiceAction.DashboardCommand("next_photo")),
                )

            lower.contains("previous photo") || lower.contains("last photo") || lower.contains("go back a photo") ->
                return VoiceTurnResult(
                    spokenText = "Going back to the previous photo.",
                    actions = listOf(VoiceAction.DashboardCommand("previous_photo")),
                )

            lower.contains("star this") || lower.contains("favorite this") || lower.contains("star photo") ->
                return VoiceTurnResult(
                    spokenText = "Updated favorite star for this photo.",
                    actions = listOf(VoiceAction.DashboardCommand("star_photo")),
                )

            lower.contains("exit fullscreen") || lower.contains("leave fullscreen") || lower.contains("close fullscreen") ->
                return VoiceTurnResult(
                    spokenText = "Returning to dashboard view.",
                    actions = listOf(VoiceAction.DashboardCommand("exit_fullscreen")),
                )

            lower.contains("fullscreen") || lower.contains("full screen") ->
                return VoiceTurnResult(
                    spokenText = "Opening fullscreen photos.",
                    actions = listOf(VoiceAction.DashboardCommand("fullscreen_photos")),
                )

            lower.contains("hide photos") || lower.contains("calendar primary") || lower.contains("full calendar") ->
                return VoiceTurnResult(
                    spokenText = "Expanding calendar view.",
                    actions = listOf(VoiceAction.DashboardCommand("show_calendar_primary")),
                )

            lower.contains("show photos") || lower.contains("bring back photos") ->
                return VoiceTurnResult(
                    spokenText = "Showing your photo slideshow.",
                    actions = listOf(VoiceAction.DashboardCommand("show_photos")),
                )

            lower.contains("dim screen") || lower.contains("go to sleep") || lower.contains("sleep screen") ->
                return VoiceTurnResult(
                    spokenText = "Dimming the screen now.",
                    actions = listOf(VoiceAction.DashboardCommand("dim_screen"), VoiceAction.EndConversation),
                    shouldEndConversation = true,
                )
        }

        // Adding Events or Tasks
        ADD_EVENT_REGEX.find(text)?.let { m ->
            val title = m.groupValues[1].trim().removePrefix("called ").removePrefix("named ").trim()
            if (title.isNotEmpty()) {
                return VoiceTurnResult(
                    spokenText = "Added event $title for today.",
                    actions = listOf(VoiceAction.AddReminder(title, ReminderCategory.EVENT, ctx.todayKey)),
                )
            }
        }
        ADD_LIST_REGEX.find(text)?.let { m ->
            val itemText = m.groupValues[1].trim()
            if (itemText.isNotEmpty()) {
                return VoiceTurnResult(
                    spokenText = "Added $itemText to your tasks.",
                    actions = listOf(VoiceAction.AddReminder(itemText, ReminderCategory.TASK, ctx.todayKey)),
                )
            }
        }
        ADD_TASK_REGEX.find(text)?.let { m ->
            val itemText = m.groupValues[1].trim()
            if (itemText.isNotEmpty()) {
                return VoiceTurnResult(
                    spokenText = "Added $itemText to your tasks.",
                    actions = listOf(VoiceAction.AddReminder(itemText, ReminderCategory.TASK, ctx.todayKey)),
                )
            }
        }
        COMPLETE_TASK_REGEX.find(text)?.let { m ->
            val query = m.groupValues[1].trim()
            if (query.isNotEmpty()) {
                return VoiceTurnResult(
                    spokenText = "Marked $query complete if it's on your list.",
                    actions = listOf(VoiceAction.CompleteReminder(query)),
                )
            }
        }

        // Morning Briefing
        if (lower.contains("briefing") || lower.contains("morning brief") || lower.contains("daily brief") || lower.contains("catch me up")) {
            val brief = buildMorningBriefing(ctx)
            return VoiceTurnResult(
                spokenText = brief,
                card = QuickAnswerCardData(question = "Morning Briefing", answer = brief),
            )
        }

        // Time / Date
        if (lower.contains("what time") || lower == "time" || lower.contains("current time")) {
            return VoiceTurnResult(spokenText = "It's ${ctx.clockTime} ${ctx.clockAmPm}.")
        }
        if (lower.contains("what day") || lower.contains("what's the date") || lower.contains("what is the date") || lower.contains("today's date")) {
            return VoiceTurnResult(spokenText = "Today is ${ctx.clockDate}.")
        }

        // Weather / Forecast / Rain
        if (lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") ||
            lower.contains("rain") || lower.contains("outside") || lower.contains("umbrella")
        ) {
            val w = ctx.weather
                ?: return VoiceTurnResult(spokenText = "Weather data is still loading for ${ctx.locationName}.")
            val today = w.daily.firstOrNull()
            val summary = buildString {
                append("In ${ctx.locationName.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}, it's currently ${w.currentTempF} degrees and ${w.description.lowercase(Locale.US)}.")
                if (today != null) {
                    append(" Today's high is ${today.maxTempF} and low is ${today.minTempF}, with a ${today.precipChance} percent chance of rain.")
                }
                if (w.alerts.isNotEmpty()) {
                    append(" There is an active ${w.alerts.first().event}.")
                }
            }
            return VoiceTurnResult(
                spokenText = summary,
                card = QuickAnswerCardData(question = "Weather · ${ctx.locationName}", answer = summary),
            )
        }

        // Schedule / Events
        if (lower.contains("next event") || lower.contains("schedule") || lower.contains("calendar") ||
            lower.contains("events") || lower.contains("appointment") || lower.contains("agenda")
        ) {
            val todayEvents = ctx.reminders.filter {
                !it.completed && it.category == ReminderCategory.EVENT && it.dateKey == ctx.todayKey
            }
            val msg = if (todayEvents.isEmpty()) {
                val upcoming = ctx.reminders.firstOrNull {
                    !it.completed && it.category == ReminderCategory.EVENT && (it.dateKey ?: "") >= ctx.todayKey
                }
                if (upcoming == null) {
                    "You have no upcoming events scheduled."
                } else {
                    "Nothing left today. Your next event is ${upcoming.text} on ${upcoming.dateKey}${upcoming.time?.let { " at $it" } ?: ""}."
                }
            } else {
                val preview = todayEvents.take(4).joinToString("; ") { ev ->
                    if (!ev.time.isNullOrBlank()) "${ev.text} at ${ev.time}" else ev.text
                }
                "Today you have ${todayEvents.size} event${if (todayEvents.size == 1) "" else "s"}: $preview."
            }
            return VoiceTurnResult(
                spokenText = msg,
                card = QuickAnswerCardData(question = "Your Schedule", answer = msg),
            )
        }

        // Tasks / Reminders
        if (lower.contains("task") || lower.contains("to do") || lower.contains("to-do") ||
            lower.contains("shopping") || lower.contains("reminder")
        ) {
            val activeTasks = ctx.reminders.filter { !it.completed && it.category == ReminderCategory.TASK }
            val msg = if (activeTasks.isEmpty()) {
                "Your task list is all clear."
            } else {
                val list = activeTasks.take(5).joinToString(", ") { it.text }
                "You have ${activeTasks.size} active task${if (activeTasks.size == 1) "" else "s"}: $list."
            }
            return VoiceTurnResult(
                spokenText = msg,
                card = QuickAnswerCardData(question = "Active Tasks", answer = msg),
            )
        }

        val helpMsg = "On-device voice handles time, weather, schedule, tasks, and screen controls. Add a Gemini API key in Settings to unlock full conversational AI and camera vision."
        return VoiceTurnResult(
            spokenText = helpMsg,
            card = QuickAnswerCardData(question = text, answer = helpMsg),
        )
    }

    fun buildMorningBriefing(ctx: VoiceContextSnapshot): String = buildString {
        append("Good morning! It's ${ctx.clockTime} ${ctx.clockAmPm} on ${ctx.clockDate}. ")
        val w = ctx.weather
        if (w != null) {
            val today = w.daily.firstOrNull()
            append("Currently ${w.currentTempF} degrees and ${w.description.lowercase(Locale.US)}")
            if (today != null) {
                append(", with a high of ${today.maxTempF}, low of ${today.minTempF}, and ${today.precipChance} percent chance of precipitation")
            }
            append(". ")
        }
        val todayEvents = ctx.reminders.filter {
            !it.completed && it.category == ReminderCategory.EVENT && it.dateKey == ctx.todayKey
        }
        if (todayEvents.isEmpty()) {
            append("Your calendar is clear today. ")
        } else {
            val items = todayEvents.take(3).joinToString("; ") { ev ->
                if (!ev.time.isNullOrBlank()) "${ev.text} at ${ev.time}" else ev.text
            }
            append("Today's events: $items. ")
        }
        val activeTasks = ctx.reminders.filter { !it.completed && it.category == ReminderCategory.TASK }
        if (activeTasks.isNotEmpty()) {
            append("You also have ${activeTasks.size} open task${if (activeTasks.size == 1) "" else "s"}, starting with ${activeTasks.first().text}.")
        }
    }.trim()
}
