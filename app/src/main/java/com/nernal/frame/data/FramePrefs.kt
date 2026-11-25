package com.nernal.frame.data

import android.content.Context
import android.content.SharedPreferences
import android.util.JsonWriter
import androidx.core.content.edit
import com.nernal.frame.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedAlbumLibrary(
    val updatedAtMs: Long,
    val totalLibraryCount: Int,
    val photos: List<AlbumPhoto>,
)

class FramePrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(BuildConfig.PREFS_NAME, Context.MODE_PRIVATE)
    private val libraryCacheFile = File(context.cacheDir, BuildConfig.LIBRARY_CACHE_FILE)
    private val localMemoryFile = File(context.filesDir, "memory_book_local.json")

    // --- Pi Sync & Favorites ---

    fun loadPiSyncEnabled(): Boolean =
        BuildConfig.IS_NERNAL_FAMILY_BUILD &&
            prefs.getBoolean(KEY_PI_SYNC_ENABLED, BuildConfig.DEFAULT_PI_SYNC_ENABLED)

    fun savePiSyncEnabled(enabled: Boolean) {
        if (!BuildConfig.IS_NERNAL_FAMILY_BUILD) return
        prefs.edit { putBoolean(KEY_PI_SYNC_ENABLED, enabled) }
    }

    fun loadPiBaseUrl(): String =
        prefs.getString(KEY_PI_BASE_URL, DEFAULT_PI_URL) ?: DEFAULT_PI_URL

    fun savePiBaseUrl(rawUrl: String): String {
        val normalized = normalizePiUrl(rawUrl)
        prefs.edit { putString(KEY_PI_BASE_URL, normalized) }
        return normalized
    }

    fun loadPendingFavOps(): List<PendingFavOp> {
        val raw = prefs.getString(KEY_PENDING_FAV_OPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("photoId")
                    if (id.isNotBlank()) {
                        add(PendingFavOp(id, o.optBoolean("favorite"), o.optLong("updatedAtMs", 0L)))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePendingFavOps(ops: List<PendingFavOp>) {
        val arr = JSONArray()
        for (op in ops) {
            arr.put(
                JSONObject().apply {
                    put("photoId", op.photoId)
                    put("favorite", op.favorite)
                    put("updatedAtMs", op.updatedAtMs)
                }
            )
        }
        prefs.edit { putString(KEY_PENDING_FAV_OPS, arr.toString()) }
    }

    // --- Hardware, Camera Wake, Audio & Gemini AI ---

    fun loadCameraMotionWakeEnabled(): Boolean =
        prefs.getBoolean(KEY_CAMERA_MOTION_WAKE, true)

    fun saveCameraMotionWakeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CAMERA_MOTION_WAKE, enabled) }
    }

    fun loadAutoSleepMinutes(): Int =
        prefs.getInt(KEY_AUTO_SLEEP_MINUTES, DEFAULT_AUTO_SLEEP_MINUTES).coerceIn(0, 60)

    fun saveAutoSleepMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_AUTO_SLEEP_MINUTES, minutes.coerceIn(0, 60)) }
    }

    fun loadAudibleEventAlertsEnabled(): Boolean =
        prefs.getBoolean(KEY_AUDIBLE_EVENT_ALERTS, true)

    fun saveAudibleEventAlertsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUDIBLE_EVENT_ALERTS, enabled) }
    }

    fun loadGeminiApiKey(): String =
        prefs.getString(KEY_GEMINI_API_KEY, "")?.trim().orEmpty()

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit { putString(KEY_GEMINI_API_KEY, apiKey.trim()) }
    }

    fun loadLocalMemoryEntries(): List<MemoryEntry> {
        if (!localMemoryFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(localMemoryFile.readText())
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val title = o.optString("title")
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        MemoryEntry(
                            id = id,
                            notebook = o.optString("notebook", "notes"),
                            title = title,
                            summary = o.optString("summary"),
                            content = o.optString("content"),
                            updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
                            fromPi = o.optBoolean("fromPi", false),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocalMemoryEntries(entries: List<MemoryEntry>) {
        try {
            val arr = JSONArray()
            for (entry in entries) {
                arr.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("notebook", entry.notebook)
                        put("title", entry.title)
                        put("summary", entry.summary)
                        put("content", entry.content)
                        put("updatedAtMs", entry.updatedAtMs)
                        put("fromPi", entry.fromPi)
                    }
                )
            }
            val tmp = File(localMemoryFile.parentFile, "${localMemoryFile.name}.tmp")
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(localMemoryFile)) {
                localMemoryFile.writeText(arr.toString())
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }

    // --- OAuth Client & Tokens ---

    fun loadOAuthClientId(): String =
        prefs.getString(KEY_OAUTH_CLIENT_ID, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_OAUTH_CLIENT_ID

    fun saveOAuthClientId(clientId: String) {
        prefs.edit { putString(KEY_OAUTH_CLIENT_ID, clientId.trim()) }
    }

    fun loadOAuthClientSecret(): String =
        prefs.getString(KEY_OAUTH_CLIENT_SECRET, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_OAUTH_CLIENT_SECRET

    fun saveOAuthClientSecret(clientSecret: String) {
        prefs.edit { putString(KEY_OAUTH_CLIENT_SECRET, clientSecret.trim()) }
    }

    fun savePendingCodeVerifier(verifier: String) {
        prefs.edit { putString(KEY_PENDING_CODE_VERIFIER, verifier.trim()) }
    }

    fun loadPendingCodeVerifier(): String? =
        prefs.getString(KEY_PENDING_CODE_VERIFIER, null)?.takeIf { it.isNotBlank() }

    fun savePendingOAuthState(state: String) {
        prefs.edit { putString(KEY_PENDING_OAUTH_STATE, state.trim()) }
    }

    fun loadPendingOAuthState(): String? =
        prefs.getString(KEY_PENDING_OAUTH_STATE, null)?.takeIf { it.isNotBlank() }

    fun savePendingRedirectUri(uri: String) {
        prefs.edit { putString(KEY_PENDING_REDIRECT_URI, uri.trim()) }
    }

    fun loadPendingRedirectUri(): String? =
        prefs.getString(KEY_PENDING_REDIRECT_URI, null)?.takeIf { it.isNotBlank() }

    fun clearPendingOAuthSession() {
        prefs.edit {
            remove(KEY_PENDING_CODE_VERIFIER)
            remove(KEY_PENDING_OAUTH_STATE)
            remove(KEY_PENDING_REDIRECT_URI)
        }
    }

    fun loadGoogleTokens(): GoogleTokens? {
        val raw = prefs.getString(KEY_GOOGLE_TOKENS, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val accessToken = obj.optString("accessToken")
            if (accessToken.isBlank()) return null
            GoogleTokens(
                accessToken = accessToken,
                refreshToken = obj.optString("refreshToken").takeIf { it.isNotBlank() },
                expiresAtMs = obj.optLong("expiresAtMs", 0L),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveGoogleTokens(tokens: GoogleTokens) {
        val obj = JSONObject().apply {
            put("accessToken", tokens.accessToken)
            put("refreshToken", tokens.refreshToken ?: "")
            put("expiresAtMs", tokens.expiresAtMs)
        }
        prefs.edit { putString(KEY_GOOGLE_TOKENS, obj.toString()) }
    }

    fun loadAccountProfile(): GoogleAccountProfile? {
        val raw = prefs.getString(KEY_ACCOUNT_PROFILE, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val email = obj.optString("email")
            if (email.isBlank()) return null
            GoogleAccountProfile(
                email = email,
                displayName = obj.optString("displayName", email),
                avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveAccountProfile(profile: GoogleAccountProfile) {
        val obj = JSONObject().apply {
            put("email", profile.email)
            put("displayName", profile.displayName)
            put("avatarUrl", profile.avatarUrl ?: "")
        }
        prefs.edit { putString(KEY_ACCOUNT_PROFILE, obj.toString()) }
    }

    fun clearGoogleAuth() {
        prefs.edit {
            remove(KEY_GOOGLE_TOKENS)
            remove(KEY_ACCOUNT_PROFILE)
            remove(KEY_SELECTED_CALENDARS)
            remove(KEY_SELECTED_TASK_LISTS)
            remove(KEY_DEFAULT_TASK_LIST_ID)
            remove(KEY_SHOPPING_TASK_LISTS)
            remove(KEY_SELECTED_PHOTO_ALBUMS)
        }
        clearPendingOAuthSession()
    }

    // --- Selected Google Sources ---

    fun hasSelectedCalendarPreference(): Boolean = prefs.contains(KEY_SELECTED_CALENDARS)

    fun loadSelectedCalendarIds(): Set<String> =
        prefs.getStringSet(KEY_SELECTED_CALENDARS, emptySet())?.toSet() ?: emptySet()

    fun saveSelectedCalendarIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_CALENDARS, ids.toSet()) }
    }

    fun hasSelectedTaskListPreference(): Boolean = prefs.contains(KEY_SELECTED_TASK_LISTS)

    fun loadSelectedTaskListIds(): Set<String> =
        prefs.getStringSet(KEY_SELECTED_TASK_LISTS, emptySet())?.toSet() ?: emptySet()

    fun saveSelectedTaskListIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_TASK_LISTS, ids.toSet()) }
    }

    fun loadDefaultTaskListId(): String? =
        prefs.getString(KEY_DEFAULT_TASK_LIST_ID, null)?.takeIf { it.isNotBlank() }

    fun saveDefaultTaskListId(listId: String?) {
        prefs.edit {
            if (listId.isNullOrBlank()) {
                remove(KEY_DEFAULT_TASK_LIST_ID)
            } else {
                putString(KEY_DEFAULT_TASK_LIST_ID, listId.trim())
            }
        }
    }

    fun loadSelectedPhotoAlbumIds(): Set<String> =
        prefs.getStringSet(KEY_SELECTED_PHOTO_ALBUMS, emptySet())?.toSet() ?: emptySet()

    fun saveSelectedPhotoAlbumIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_PHOTO_ALBUMS, ids.toSet()) }
    }

    fun loadCustomSharedAlbumUrls(): List<String> {
        val defaultUrls = defaultAlbumUrls()
        if (!prefs.contains(KEY_CUSTOM_SHARED_ALBUM_URLS)) {
            return defaultUrls
        }
        val raw = prefs.getString(KEY_CUSTOM_SHARED_ALBUM_URLS, null) ?: return defaultUrls
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i).trim()
                    if (u.isNotBlank()) add(u)
                }
            }
        } catch (_: Exception) {
            defaultUrls
        }
    }

    fun saveCustomSharedAlbumUrls(urls: List<String>) {
        val arr = JSONArray()
        for (u in urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()) {
            arr.put(u)
        }
        prefs.edit { putString(KEY_CUSTOM_SHARED_ALBUM_URLS, arr.toString()) }
    }

    // --- Location ---

    fun loadUserLocation(): UserLocation? {
        val raw = prefs.getString(KEY_USER_LOCATION, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val name = obj.optString("name")
            if (name.isBlank()) return null
            UserLocation(
                name = name,
                latitude = obj.getDouble("latitude"),
                longitude = obj.getDouble("longitude"),
                timezone = obj.optString("timezone", "America/New_York").ifBlank { "America/New_York" },
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveUserLocation(loc: UserLocation) {
        val obj = JSONObject().apply {
            put("name", loc.name)
            put("latitude", loc.latitude)
            put("longitude", loc.longitude)
            put("timezone", loc.timezone)
        }
        prefs.edit { putString(KEY_USER_LOCATION, obj.toString()) }
    }

    // --- Per-device display preferences ---

    fun loadSlideIntervalSec(): Int =
        prefs.getInt(KEY_SLIDE_INTERVAL_SEC, DEFAULT_SLIDE_INTERVAL_SEC).coerceIn(5, 120)

    fun saveSlideIntervalSec(seconds: Int) {
        prefs.edit { putInt(KEY_SLIDE_INTERVAL_SEC, seconds.coerceIn(5, 120)) }
    }

    fun loadPhotoYearFilters(): Set<Int>? {
        val raw = runCatching { prefs.getString(KEY_PHOTO_YEAR_FILTERS, null) }.getOrNull()
        if (raw != null) {
            if (raw == "NONE") return emptySet()
            return raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 1970 }
                .toSet()
                .takeIf { it.isNotEmpty() }
        }
        if (BuildConfig.IS_NERNAL_FAMILY_BUILD) {
            return prefs.getInt(KEY_LEGACY_PHOTO_YEAR_FILTER, 0)
                .takeIf { it > 1970 }
                ?.let { setOf(it) }
        }
        return null
    }

    fun savePhotoYearFilters(years: Set<Int>?) {
        prefs.edit {
            when {
                years == null -> remove(KEY_PHOTO_YEAR_FILTERS)
                years.isEmpty() -> putString(KEY_PHOTO_YEAR_FILTERS, "NONE")
                else -> putString(KEY_PHOTO_YEAR_FILTERS, years.sorted().joinToString(","))
            }
            if (BuildConfig.IS_NERNAL_FAMILY_BUILD) {
                remove(KEY_LEGACY_PHOTO_YEAR_FILTER)
            }
        }
    }

    fun loadActiveTaskListFilters(): Set<String>? {
        val raw = prefs.getString(KEY_ACTIVE_TASK_LIST_FILTERS, null) ?: return null
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet().takeIf { it.isNotEmpty() }
    }

    fun saveActiveTaskListFilters(listIds: Set<String>?) {
        prefs.edit {
            if (listIds == null) {
                remove(KEY_ACTIVE_TASK_LIST_FILTERS)
            } else {
                putString(KEY_ACTIVE_TASK_LIST_FILTERS, listIds.joinToString(","))
            }
        }
    }

    fun loadPhotoAlbumFilter(): String? =
        prefs.getString(KEY_PHOTO_ALBUM_FILTER, null)?.takeIf { it.isNotBlank() }

    fun savePhotoAlbumFilter(albumTitle: String?) {
        prefs.edit { putString(KEY_PHOTO_ALBUM_FILTER, albumTitle ?: "") }
    }

    fun loadDimBrightness(): Float =
        prefs.getFloat(KEY_DIM_BRIGHTNESS, DEFAULT_DIM_BRIGHTNESS).coerceIn(0.06f, 0.45f)

    fun saveDimBrightness(brightness: Float) {
        prefs.edit { putFloat(KEY_DIM_BRIGHTNESS, brightness.coerceIn(0.06f, 0.45f)) }
    }

    fun loadGlobalBrightness(): Float =
        prefs.getFloat(KEY_GLOBAL_BRIGHTNESS, DEFAULT_GLOBAL_BRIGHTNESS).coerceIn(0.1f, 1.0f)

    fun saveGlobalBrightness(brightness: Float) {
        prefs.edit { putFloat(KEY_GLOBAL_BRIGHTNESS, brightness.coerceIn(0.1f, 1.0f)) }
    }

    fun loadWakeOnEventReminder(): Boolean =
        prefs.getBoolean(KEY_WAKE_ON_EVENT_REMINDER, true)

    fun saveWakeOnEventReminder(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_WAKE_ON_EVENT_REMINDER, enabled) }
    }

    // --- Reminders & Offline Queue ---

    fun loadReminders(): List<ReminderItem> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val catName = obj.optString("category", ReminderCategory.TASK.name)
                    val category = runCatching { ReminderCategory.valueOf(catName) }
                        .getOrDefault(ReminderCategory.TASK)
                    add(
                        ReminderItem(
                            id = obj.optString("id"),
                            text = obj.optString("text"),
                            category = category,
                            dateKey = obj.optString("dateKey").takeIf { it.isNotBlank() },
                            time = obj.optString("time").takeIf { it.isNotBlank() },
                            completed = obj.optBoolean("completed", false),
                            createdAtMs = obj.optLong("createdAtMs", 0L),
                            sourceId = obj.optString("sourceId").takeIf { it.isNotBlank() },
                            remoteId = obj.optString("remoteId").takeIf { it.isNotBlank() },
                            notes = obj.optString("notes").takeIf { it.isNotBlank() },
                            color = obj.optString("color").takeIf { it.isNotBlank() } ?: obj.optString("c").takeIf { it.isNotBlank() },
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveReminders(items: List<ReminderItem>) {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("category", item.category.name)
                    put("dateKey", item.dateKey ?: "")
                    put("time", item.time ?: "")
                    put("completed", item.completed)
                    put("createdAtMs", item.createdAtMs)
                    put("sourceId", item.sourceId ?: "")
                    put("remoteId", item.remoteId ?: "")
                    put("notes", item.notes ?: "")
                    put("color", item.color ?: "")
                }
            )
        }
        prefs.edit { putString(KEY_REMINDERS, arr.toString()) }
    }

    fun loadStarredIds(): Set<String> =
        prefs.getStringSet(KEY_STARRED_IDS, emptySet())?.toSet() ?: emptySet()

    fun saveStarredIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_STARRED_IDS, ids.toSet()) }
    }

    fun loadPendingReminderOps(): List<PendingReminderOp> {
        val raw = prefs.getString(KEY_PENDING_REM_OPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val action = runCatching { PendingReminderAction.valueOf(o.optString("action")) }
                        .getOrNull() ?: continue
                    val cat = runCatching { ReminderCategory.valueOf(o.optString("category", "TASK")) }
                        .getOrDefault(ReminderCategory.TASK)
                    add(
                        PendingReminderOp(
                            opId = o.optString("opId"),
                            action = action,
                            reminderId = o.optString("reminderId"),
                            text = o.optString("text"),
                            time = o.optString("time").takeIf { it.isNotBlank() },
                            dateKey = o.optString("dateKey").takeIf { it.isNotBlank() },
                            category = cat,
                            completed = o.optBoolean("completed", false),
                            sourceId = o.optString("sourceId").takeIf { it.isNotBlank() },
                            remoteId = o.optString("remoteId").takeIf { it.isNotBlank() },
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePendingReminderOps(ops: List<PendingReminderOp>) {
        val arr = JSONArray()
        for (op in ops) {
            arr.put(
                JSONObject().apply {
                    put("opId", op.opId)
                    put("action", op.action.name)
                    put("reminderId", op.reminderId)
                    put("text", op.text)
                    put("time", op.time ?: "")
                    put("dateKey", op.dateKey ?: "")
                    put("category", op.category.name)
                    put("completed", op.completed)
                    put("sourceId", op.sourceId ?: "")
                    put("remoteId", op.remoteId ?: "")
                }
            )
        }
        prefs.edit { putString(KEY_PENDING_REM_OPS, arr.toString()) }
    }

    // --- Photo Library Cache ---

    fun loadAlbumLibrary(): CachedAlbumLibrary {
        if (!libraryCacheFile.exists()) return CachedAlbumLibrary(0L, 0, emptyList())
        return try {
            val root = JSONObject(libraryCacheFile.readText())
            val updatedAt = root.optLong("updatedAt", 0L)
            val totalCount = root.optInt("totalCount", 0)
            val arr = root.optJSONArray("photos") ?: JSONArray()
            val photos = buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val yr = o.optInt("y", 0)
                    add(
                        AlbumPhoto(
                            id = o.optString("i"),
                            url = o.optString("u"),
                            width = o.optInt("w", 1920),
                            height = o.optInt("h", 1080),
                            timestampMs = o.optLong("t", 0L).takeIf { it > 0L },
                            year = yr.takeIf { it > 1970 },
                            albumTitle = o.optString("a", "Google Photos"),
                            albumUrl = o.optString("au", ""),
                        )
                    )
                }
            }
            CachedAlbumLibrary(updatedAt, maxOf(totalCount, photos.size), photos)
        } catch (_: Exception) {
            CachedAlbumLibrary(0L, 0, emptyList())
        }
    }

    fun saveAlbumLibrary(
        photos: List<AlbumPhoto>,
        totalLibraryCount: Int = photos.size,
        updatedAtMs: Long = System.currentTimeMillis(),
    ) {
        try {
            val tmp = File(libraryCacheFile.parentFile, "${libraryCacheFile.name}.tmp")
            JsonWriter(tmp.bufferedWriter(Charsets.UTF_8)).use { writer ->
                writer.beginObject()
                writer.name("updatedAt").value(updatedAtMs)
                writer.name("totalCount").value(maxOf(totalLibraryCount, photos.size))
                writer.name("photos")
                writer.beginArray()
                for (p in photos) {
                    writer.beginObject()
                    writer.name("i").value(p.id)
                    writer.name("u").value(p.url)
                    writer.name("w").value(p.width)
                    writer.name("h").value(p.height)
                    writer.name("t").value(p.timestampMs ?: 0L)
                    writer.name("y").value(p.year ?: 0)
                    writer.name("a").value(p.albumTitle)
                    writer.name("au").value(p.albumUrl)
                    writer.endObject()
                }
                writer.endArray()
                writer.endObject()
            }
            tmp.renameTo(libraryCacheFile)
        } catch (_: Exception) {
        }
    }

    fun clearAlbumLibrary() {
        runCatching { libraryCacheFile.delete() }
    }

    fun loadCalendarViewMode(): CalendarViewMode {
        val raw = prefs.getString(KEY_CALENDAR_VIEW_MODE, null) ?: return CalendarViewMode.ITINERARY
        return runCatching { CalendarViewMode.valueOf(raw) }.getOrDefault(CalendarViewMode.ITINERARY)
    }

    fun saveCalendarViewMode(mode: CalendarViewMode) {
        prefs.edit { putString(KEY_CALENDAR_VIEW_MODE, mode.name) }
    }

    fun loadCalendarPrimaryMode(): Boolean =
        prefs.getBoolean(KEY_CALENDAR_PRIMARY_MODE, false)

    fun saveCalendarPrimaryMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CALENDAR_PRIMARY_MODE, enabled) }
    }

    fun loadLastAddCategory(): ReminderCategory {
        val raw = prefs.getString(KEY_LAST_ADD_CATEGORY, null) ?: return ReminderCategory.TASK
        return runCatching { ReminderCategory.valueOf(raw) }.getOrDefault(ReminderCategory.TASK)
    }

    fun saveLastAddCategory(category: ReminderCategory) {
        prefs.edit { putString(KEY_LAST_ADD_CATEGORY, category.name) }
    }

    fun hasPromptedOverlayPermission(): Boolean =
        prefs.getBoolean(KEY_OVERLAY_PERMISSION_PROMPTED, false)

    fun setPromptedOverlayPermission(prompted: Boolean) {
        prefs.edit { putBoolean(KEY_OVERLAY_PERMISSION_PROMPTED, prompted) }
    }

    fun loadWeatherCollapsed(): Boolean =
        prefs.getBoolean(KEY_WEATHER_COLLAPSED, false)

    fun saveWeatherCollapsed(collapsed: Boolean) {
        prefs.edit { putBoolean(KEY_WEATHER_COLLAPSED, collapsed) }
    }

    fun loadDismissedAlertKeys(): Set<String> =
        prefs.getStringSet(KEY_DISMISSED_ALERT_KEYS, emptySet())?.toSet() ?: emptySet()

    fun saveDismissedAlertKeys(keys: Set<String>) {
        prefs.edit { putStringSet(KEY_DISMISSED_ALERT_KEYS, keys.toSet()) }
    }

    fun loadAlarmConfig(): AlarmConfig {
        val raw = prefs.getString(KEY_ALARM_CONFIG, null) ?: return AlarmConfig()
        return try {
            val obj = JSONObject(raw)
            val daysArr = obj.optJSONArray("daysOfWeek")
            val days = if (daysArr != null) {
                buildSet {
                    for (i in 0 until daysArr.length()) {
                        val d = daysArr.optInt(i, 0)
                        if (d in 1..7) add(d)
                    }
                }.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
            } else {
                setOf(1, 2, 3, 4, 5, 6, 7)
            }
            val timing = runCatching {
                AlarmBriefTiming.valueOf(obj.optString("speakBriefTiming", AlarmBriefTiming.ON_DISMISS.name))
            }.getOrDefault(AlarmBriefTiming.ON_DISMISS)
            AlarmConfig(
                enabled = obj.optBoolean("enabled", false),
                hour24 = obj.optInt("hour24", 7).coerceIn(0, 23),
                minute = obj.optInt("minute", 0).coerceIn(0, 59),
                daysOfWeek = days,
                speakBriefTiming = timing,
                label = obj.optString("label", "Morning Alarm").ifBlank { "Morning Alarm" },
            )
        } catch (_: Exception) {
            AlarmConfig()
        }
    }

    fun saveAlarmConfig(config: AlarmConfig) {
        val daysArr = JSONArray()
        config.daysOfWeek.sorted().forEach { daysArr.put(it) }
        val obj = JSONObject().apply {
            put("enabled", config.enabled)
            put("hour24", config.hour24.coerceIn(0, 23))
            put("minute", config.minute.coerceIn(0, 59))
            put("daysOfWeek", daysArr)
            put("speakBriefTiming", config.speakBriefTiming.name)
            put("label", config.label.ifBlank { "Morning Alarm" })
        }
        prefs.edit { putString(KEY_ALARM_CONFIG, obj.toString()) }
    }

    companion object {
        const val DEFAULT_PI_URL = "http://192.168.68.86:3000"
        const val DEFAULT_SLIDE_INTERVAL_SEC = 10
        const val DEFAULT_DIM_BRIGHTNESS = 0.16f
        const val DEFAULT_GLOBAL_BRIGHTNESS = 0.85f
        const val DEFAULT_AUTO_SLEEP_MINUTES = 10

        private const val KEY_PI_SYNC_ENABLED = "pi_sync_enabled_v1"
        private const val KEY_PI_BASE_URL = "pi_base_url_v1"
        private const val KEY_PENDING_FAV_OPS = "pending_fav_ops_v1"

        private const val KEY_CAMERA_MOTION_WAKE = "camera_motion_wake_v1"
        private const val KEY_AUTO_SLEEP_MINUTES = "auto_sleep_minutes_v1"
        private const val KEY_AUDIBLE_EVENT_ALERTS = "audible_event_alerts_v1"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_v1"

        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id_v1"
        private const val KEY_OAUTH_CLIENT_SECRET = "oauth_client_secret_v1"
        private const val KEY_PENDING_CODE_VERIFIER = "pending_code_verifier_v1"
        private const val KEY_PENDING_OAUTH_STATE = "pending_oauth_state_v1"
        private const val KEY_PENDING_REDIRECT_URI = "pending_redirect_uri_v1"
        private const val KEY_GOOGLE_TOKENS = "google_tokens_v1"
        private const val KEY_ACCOUNT_PROFILE = "google_account_profile_v1"
        private const val KEY_SELECTED_CALENDARS = "selected_calendar_ids_v1"
        private const val KEY_SELECTED_TASK_LISTS = "selected_task_list_ids_v1"
        private const val KEY_DEFAULT_TASK_LIST_ID = "default_task_list_id_v1"
        private const val KEY_SHOPPING_TASK_LISTS = "shopping_task_list_ids_v1"
        private const val KEY_SELECTED_PHOTO_ALBUMS = "selected_photo_album_ids_v1"
        private const val KEY_CUSTOM_SHARED_ALBUM_URLS = "custom_shared_album_urls_v1"
        private const val KEY_USER_LOCATION = "user_location_v1"

        private const val KEY_SLIDE_INTERVAL_SEC = "local_slide_interval_sec_v1"
        private const val KEY_LEGACY_PHOTO_YEAR_FILTER = "local_photo_year_v1"
        private const val KEY_PHOTO_YEAR_FILTERS = "local_photo_years_v2"
        private const val KEY_PHOTO_ALBUM_FILTER = "local_photo_album_v1"
        private const val KEY_DIM_BRIGHTNESS = "local_dim_brightness_v1"
        private const val KEY_GLOBAL_BRIGHTNESS = "global_brightness_v1"
        private const val KEY_WAKE_ON_EVENT_REMINDER = "wake_on_event_reminder_v1"
        private const val KEY_CALENDAR_VIEW_MODE = "calendar_view_mode_v1"
        private const val KEY_CALENDAR_PRIMARY_MODE = "calendar_primary_mode_v1"
        private const val KEY_LAST_ADD_CATEGORY = "last_add_category_v1"
        private const val KEY_OVERLAY_PERMISSION_PROMPTED = "overlay_permission_prompted_v1"
        private const val KEY_WEATHER_COLLAPSED = "weather_collapsed_v1"
        private const val KEY_DISMISSED_ALERT_KEYS = "dismissed_alert_keys_v1"
        private const val KEY_ALARM_CONFIG = "alarm_config_v1"
        private val KEY_REMINDERS = BuildConfig.REMINDERS_PREF_KEY
        private const val KEY_STARRED_IDS = "starred_photo_ids_v1"
        private val KEY_PENDING_REM_OPS = BuildConfig.PENDING_REM_OPS_KEY
        private const val KEY_ACTIVE_TASK_LIST_FILTERS = "active_task_list_filters_v1"

        fun defaultAlbumUrls(): List<String> =
            BuildConfig.DEFAULT_ALBUM_URLS_CSV
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

        fun normalizePiUrl(raw: String): String {
            var trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return DEFAULT_PI_URL
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "http://$trimmed"
            }
            val hostPart = trimmed.substringAfter("://").substringBefore('/')
            if (!hostPart.contains(":")) {
                val pathPart = trimmed.substringAfter(hostPart, "")
                val scheme = trimmed.substringBefore("://")
                trimmed = "$scheme://$hostPart:3000$pathPart"
            }
            return trimmed
        }
    }
}
