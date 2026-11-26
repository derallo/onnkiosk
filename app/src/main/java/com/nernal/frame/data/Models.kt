package com.nernal.frame.data

import androidx.compose.ui.graphics.Color
import com.nernal.frame.BuildConfig
import com.nernal.frame.ui.NernalColors

enum class WeatherConditionKind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    SNOW,
    STORM,
}

data class HourlyForecast(
    val timeIso: String,
    val dateIso: String,
    val hour24: Int,
    val weatherCode: Int,
    val tempF: Int,
    val apparentTempF: Int,
    val precipChance: Int,
    val precipInches: Double,
    val windMph: Int,
    val humidityPercent: Int,
)

data class DailyForecast(
    val dateIso: String,
    val dayLabel: String,
    val weatherCode: Int,
    val maxTempF: Int,
    val minTempF: Int,
    val precipChance: Int,
    val apparentMaxTempF: Int? = null,
    val apparentMinTempF: Int? = null,
    val precipSumInches: Double = 0.0,
    val maxWindMph: Int? = null,
    val maxWindGustsMph: Int? = null,
    val windDirectionDeg: Int? = null,
    val uvIndexMax: Double? = null,
    val sunriseIso: String = "",
    val sunsetIso: String = "",
)

data class WeatherAlert(
    val event: String,
    val headline: String,
    val severity: String,
    val description: String,
) {
    fun alertKey(): String = "${event.trim()}|${headline.trim()}"
}

data class GeminiKeyCandidate(
    val taskId: String,
    val taskTitle: String,
    val apiKey: String,
) {
    val maskedKey: String
        get() = if (apiKey.length <= 12) apiKey else "${apiKey.take(8)}…${apiKey.takeLast(4)}"
}

enum class AlarmBriefTiming(val label: String) {
    ON_DISMISS("When Silenced"),
    AT_ALARM("At Alarm Time"),
    OFF("Chime Only"),
}

data class AlarmConfig(
    val enabled: Boolean = false,
    val hour24: Int = 7,
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val speakBriefTiming: AlarmBriefTiming = AlarmBriefTiming.ON_DISMISS,
    val label: String = "Morning Alarm",
) {
    val formattedTime: String
        get() {
            val amPm = if (hour24 < 12) "AM" else "PM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
        }

    fun nextOccurrenceLabel(now: java.time.ZonedDateTime): String? {
        if (!enabled || daysOfWeek.isEmpty()) return null
        val alarmMinutes = hour24 * 60 + minute
        val nowMinutes = now.hour * 60 + now.minute
        for (offset in 0..7) {
            val candidate = now.plusDays(offset.toLong())
            if (candidate.dayOfWeek.value !in daysOfWeek) continue
            if (offset == 0 && alarmMinutes <= nowMinutes) continue
            val dayPrefix = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> candidate.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)
            }
            return "$dayPrefix · $formattedTime"
        }
        return formattedTime
    }
}

data class WeatherData(
    val currentTempF: Int,
    val apparentTempF: Int,
    val humidityPercent: Int,
    val windMph: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val description: String,
    val precipInches: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val daily: List<DailyForecast>,
    val alerts: List<WeatherAlert>,
    val fetchedAtMs: Long,
    val windGustsMph: Int? = null,
    val windDirectionDeg: Int? = null,
    val cloudCoverPercent: Int? = null,
    val uvIndex: Double? = null,
    val hourly: List<HourlyForecast> = emptyList(),
)

data class RadarFrame(
    val timeSec: Long,
    val path: String,
    val isForecast: Boolean,
)

data class RadarData(
    val host: String,
    val frames: List<RadarFrame>,
    val fetchedAtMs: Long,
)

data class AlbumPhoto(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val timestampMs: Long?,
    val year: Int?,
    val albumTitle: String,
    val albumUrl: String = "",
) {
    /** Serves appropriately sized imagery from Google's CDN (shared links & Google Photos API baseUrl) to preserve device RAM. */
    fun displayUrl(maxWidth: Int = 1280, maxHeight: Int = 800): String {
        val lastSegment = url.substringAfterLast('/')
        return if (url.contains("googleusercontent.com") && !lastSegment.contains("=") && !url.contains("?")) {
            "$url=w$maxWidth-h$maxHeight"
        } else {
            url
        }
    }
}

enum class ReminderCategory(val label: String, val pluralLabel: String, val piValue: String) {
    EVENT("Event", "Events", "calendar"),
    TASK("Task", "Tasks", "task");

    companion object {
        fun fromPi(raw: String?): ReminderCategory = when (raw?.lowercase()) {
            "calendar", "event" -> EVENT
            else -> TASK
        }
    }
}

enum class PiConnectionState {
    CHECKING,
    CONNECTED,
    STANDALONE,
}

data class PendingFavOp(
    val photoId: String,
    val favorite: Boolean,
    val updatedAtMs: Long = System.currentTimeMillis(),
)

data class DeviceCapabilities(
    val hasCamera: Boolean = false,
    val hasFrontCamera: Boolean = false,
    val preferredCameraId: String? = null,
    val hasMicrophone: Boolean = false,
    val hasAudioOutput: Boolean = false,
    val hasOnDeviceSpeech: Boolean = false,
    val hasTextToSpeech: Boolean = false,
) {
    val hasVoiceInputAndOutput: Boolean
        get() = hasMicrophone && hasAudioOutput && hasOnDeviceSpeech
}

enum class VoiceAssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
}

data class QuickAnswerCardData(
    val question: String,
    val answer: String,
    val sources: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
)

data class MemoryEntry(
    val id: String,
    val notebook: String,
    val title: String,
    val summary: String,
    val content: String,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val fromPi: Boolean = false,
)

enum class CalendarViewMode(val label: String) {
    ITINERARY("Itinerary"),
    THREE_DAY("3-Day"),
    WEEK("Week"),
    MONTH("Month"),
    NONE("None"),
}

data class ReminderItem(
    val id: String,
    val text: String,
    val category: ReminderCategory,
    val dateKey: String? = null,
    val time: String? = null,
    val completed: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val sourceId: String? = null,
    val remoteId: String? = null,
    val notes: String? = null,
    val color: String? = null,
) {
    val startTimeMinutes: Int?
        get() = parseTimeLabelToMinutes(time)

    val isAllDay: Boolean
        get() = category == ReminderCategory.EVENT && (time.isNullOrBlank() || time.equals("All day", ignoreCase = true))
}

fun parseHexColor(hex: String): Color? {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return null
        }
        Color(colorInt)
    }.getOrNull()
}

fun eventColor(item: ReminderItem, availableCalendars: List<GoogleCalendarInfo>): Color {
    val hex = item.color
        ?: availableCalendars.find { it.id == item.sourceId }?.backgroundColor
    if (!hex.isNullOrBlank()) {
        val parsed = parseHexColor(hex)
        if (parsed != null) return parsed
    }
    return NernalColors.Accent
}

fun parseTimeLabelToMinutes(timeStr: String?): Int? {
    val raw = timeStr?.trim()?.uppercase(java.util.Locale.US)?.takeIf { it.isNotEmpty() } ?: return null
    val isPm = raw.endsWith("PM")
    val isAm = raw.endsWith("AM")
    val clean = raw.removeSuffix("PM").removeSuffix("AM").trim()
    val parts = clean.split(":")
    if (parts.isEmpty() || parts.size > 2) return null
    val rawHour = parts[0].trim().toIntOrNull() ?: return null
    val minute = if (parts.size == 2) parts[1].trim().toIntOrNull() ?: return null else 0
    if (minute !in 0..59) return null
    val hour24 = when {
        isAm || isPm -> {
            if (rawHour !in 1..12) return null
            when {
                isPm && rawHour < 12 -> rawHour + 12
                isAm && rawHour == 12 -> 0
                else -> rawHour
            }
        }
        else -> {
            if (rawHour !in 0..23) return null
            rawHour
        }
    }
    return hour24 * 60 + minute
}

fun sortReminders(items: List<ReminderItem>): List<ReminderItem> =
    items.sortedWith(
        compareBy<ReminderItem> { it.completed }
            .thenBy { it.dateKey ?: "9999-99-99" }
            .thenBy { if (it.dateKey != null) (if (it.isAllDay || it.startTimeMinutes == null) -1 else it.startTimeMinutes!!) else Int.MAX_VALUE }
            .thenBy { if (it.category == ReminderCategory.EVENT) 0 else 1 }
            .thenByDescending { it.createdAtMs }
    )

fun selectNextUpcomingEvent(
    reminders: List<ReminderItem>,
    todayKey: String,
    nowMinutes: Int? = null,
): ReminderItem? {
    val candidates = reminders.filter {
        !it.completed &&
            it.category == ReminderCategory.EVENT &&
            it.dateKey != null &&
            it.dateKey >= todayKey
    }
    if (candidates.isEmpty()) return null
    val preferred = if (nowMinutes == null) {
        candidates
    } else {
        candidates.filter { item ->
            val d = item.dateKey!!
            d > todayKey || item.startTimeMinutes == null || item.startTimeMinutes!! >= nowMinutes
        }.ifEmpty { candidates }
    }
    return preferred.minWithOrNull(
        compareBy<ReminderItem>(
            { it.dateKey ?: "9999-99-99" },
            { it.startTimeMinutes ?: Int.MAX_VALUE },
            { it.createdAtMs },
        )
    )
}

fun isEventWithinReminderWindow(
    item: ReminderItem,
    todayKey: String,
    nowMinutes: Int,
): Boolean {
    if (item.category != ReminderCategory.EVENT || item.completed || item.dateKey != todayKey) return false
    val eventMinutes = item.startTimeMinutes ?: return false
    return (eventMinutes - nowMinutes) in 1..5
}

fun shouldAutoCloseEventAlert(
    alert: ReminderItem,
    reminders: List<ReminderItem>,
    todayKey: String,
    nowMinutes: Int,
    wakeEnabled: Boolean,
): Boolean {
    if (!wakeEnabled) return true
    val matching = reminders.firstOrNull { it.id == alert.id && !it.completed } ?: return true
    if (matching.dateKey != todayKey) return true
    val eventMinutes = matching.startTimeMinutes ?: return true
    return nowMinutes >= eventMinutes
}

sealed interface RemoteMutationResult<out T> {
    data class Success<T>(val value: T) : RemoteMutationResult<T>
    data class TransientError(val message: String? = null) : RemoteMutationResult<Nothing>
    data class PermanentError(val httpCode: Int, val message: String) : RemoteMutationResult<Nothing>
}

enum class GoogleAuthState {
    NOT_SIGNED_IN,
    SIGNING_IN,
    SIGNED_IN,
    ERROR,
}

data class GoogleAccountProfile(
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

data class GoogleTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMs: Long,
) {
    fun isExpiredOrExpiringSoon(nowMs: Long = System.currentTimeMillis(), bufferMs: Long = 60_000L): Boolean =
        accessToken.isBlank() || (expiresAtMs > 0L && nowMs + bufferMs >= expiresAtMs)
}

data class GoogleCalendarInfo(
    val id: String,
    val summary: String,
    val backgroundColor: String? = null,
    val primary: Boolean = false,
    val accessRole: String = "reader",
) {
    val isWritable: Boolean
        get() = accessRole == "owner" || accessRole == "writer"
}

data class GoogleTaskListInfo(
    val id: String,
    val title: String,
)

data class GooglePhotoAlbumInfo(
    val id: String,
    val title: String,
    val mediaItemsCount: Int = 0,
    val coverPhotoBaseUrl: String? = null,
    val isShared: Boolean = false,
)

data class UserLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
) {
    companion object {
        val DEFAULT = UserLocation(
            name = BuildConfig.DEFAULT_LOCATION_NAME,
            latitude = BuildConfig.DEFAULT_LAT.toDoubleOrNull() ?: 38.8951,
            longitude = BuildConfig.DEFAULT_LON.toDoubleOrNull() ?: -77.0364,
            timezone = "America/New_York",
        )
    }
}

enum class PendingReminderAction { ADD, UPDATE, DELETE }

data class PendingReminderOp(
    val opId: String,
    val action: PendingReminderAction,
    val reminderId: String,
    val text: String = "",
    val time: String? = null,
    val dateKey: String? = null,
    val category: ReminderCategory = ReminderCategory.TASK,
    val completed: Boolean = false,
    val sourceId: String? = null,
    val remoteId: String? = null,
)

fun wmoDescription(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mainly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing Drizzle"
    61, 63 -> "Rain"
    65 -> "Heavy Rain"
    66, 67 -> "Freezing Rain"
    71, 73 -> "Snow"
    75 -> "Heavy Snow"
    77 -> "Snow Grains"
    80, 81, 82 -> "Rain Showers"
    85, 86 -> "Snow Showers"
    95 -> "Thunderstorm"
    96, 99 -> "Hail Thunderstorm"
    else -> "Clear"
}

fun wmoKind(code: Int): WeatherConditionKind = when (code) {
    0, 1 -> WeatherConditionKind.CLEAR
    2 -> WeatherConditionKind.PARTLY_CLOUDY
    3 -> WeatherConditionKind.CLOUDY
    45, 48 -> WeatherConditionKind.FOG
    in 51..67, in 80..82 -> WeatherConditionKind.RAIN
    in 71..77, 85, 86 -> WeatherConditionKind.SNOW
    in 95..99 -> WeatherConditionKind.STORM
    else -> WeatherConditionKind.PARTLY_CLOUDY
}

fun windDirectionCompass(degrees: Int?): String {
    if (degrees == null) return ""
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val norm = ((degrees % 360) + 360) % 360
    return dirs[((norm + 22) / 45) % 8]
}

fun uvIndexCategory(uv: Double): String = when {
    uv < 3.0 -> "Low"
    uv < 6.0 -> "Moderate"
    uv < 8.0 -> "High"
    uv < 11.0 -> "Very High"
    else -> "Extreme"
}

fun formatWeatherTimeIso(iso: String): String {
    val clean = iso.trim()
    if (clean.isEmpty()) return ""
    return runCatching {
        val timePart = clean.substringAfter('T', clean).take(5)
        val parts = timePart.split(":")
        val hour24 = parts[0].toInt()
        val minute = parts[1].toInt()
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    }.getOrDefault(clean)
}

fun buildWeatherAssessment(
    weather: WeatherData,
    day: DailyForecast,
    isToday: Boolean,
): String {
    val conditionDesc = if (isToday) weather.description else wmoDescription(day.weatherCode)
    val sb = StringBuilder()
    if (isToday) {
        sb.append("Currently ${weather.currentTempF}°F (${conditionDesc.lowercase(java.util.Locale.US)}), feeling like ${weather.apparentTempF}°F. ")
        sb.append("Today's forecast reaches a high of ${day.maxTempF}°F with a low of ${day.minTempF}°F. ")
    } else {
        val dayName = runCatching {
            java.time.LocalDate.parse(day.dateIso).dayOfWeek
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
        }.getOrDefault(day.dayLabel)
        sb.append("$dayName looks ${conditionDesc.lowercase(java.util.Locale.US)} with a high of ${day.maxTempF}°F and a low of ${day.minTempF}°F")
        if (day.apparentMaxTempF != null && kotlin.math.abs(day.apparentMaxTempF - day.maxTempF) >= 3) {
            sb.append(" (feeling near ${day.apparentMaxTempF}°F)")
        }
        sb.append(". ")
    }

    val rainText = when {
        day.precipChance >= 60 -> "Rain is likely (${day.precipChance}% chance" +
            (if (day.precipSumInches >= 0.05) ", around ${String.format(java.util.Locale.US, "%.2f", day.precipSumInches)} in expected" else "") +
            "). "
        day.precipChance >= 25 -> "There is a ${day.precipChance}% chance of precipitation" +
            (if (day.precipSumInches >= 0.05) " (${String.format(java.util.Locale.US, "%.2f", day.precipSumInches)} in)" else "") +
            ". "
        else -> "Precipitation looks unlikely (${day.precipChance}% chance). "
    }
    sb.append(rainText)

    val windVal = if (isToday) weather.windMph else (day.maxWindMph ?: weather.windMph)
    val gustVal = if (isToday) weather.windGustsMph else day.maxWindGustsMph
    val dirText = windDirectionCompass(if (isToday) weather.windDirectionDeg else day.windDirectionDeg)
    if (windVal >= 15 || (gustVal != null && gustVal >= 25)) {
        sb.append("Winds ${if (dirText.isNotEmpty()) "from the $dirText " else ""}around $windVal mph")
        if (gustVal != null && gustVal > windVal + 4) {
            sb.append(" with gusts up to $gustVal mph")
        }
        sb.append(". ")
    }

    val callouts = mutableListOf<String>()
    val uvVal = if (isToday) (day.uvIndexMax ?: weather.uvIndex) else day.uvIndexMax
    if (uvVal != null && uvVal >= 6.0) {
        callouts.add("high UV (${uvIndexCategory(uvVal).lowercase(java.util.Locale.US)})")
    }
    if (day.minTempF <= 32) {
        callouts.add("freezing overnight lows (${day.minTempF}°F)")
    }
    if (day.precipChance >= 50) {
        callouts.add("keep an umbrella handy")
    }
    if (callouts.isNotEmpty()) {
        sb.append("Heads up: ${callouts.joinToString(", ")}.")
    }

    return sb.toString().trim()
}
