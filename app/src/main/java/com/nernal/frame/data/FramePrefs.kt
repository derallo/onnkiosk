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
        context.getSharedPreferences("nernal_oauth_frame_prefs", Context.MODE_PRIVATE)
    private val libraryCacheFile = File(context.cacheDir, "oauth_album_library_cache.json")

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
            remove(KEY_SHOPPING_TASK_LISTS)
            remove(KEY_SELECTED_PHOTO_ALBUMS)
            remove(KEY_REMINDERS)
            remove(KEY_PENDING_REM_OPS)
        }
        clearPendingOAuthSession()
        clearAlbumLibrary()
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

    fun loadSelectedPhotoAlbumIds(): Set<String> =
        prefs.getStringSet(KEY_SELECTED_PHOTO_ALBUMS, emptySet())?.toSet() ?: emptySet()

    fun saveSelectedPhotoAlbumIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_PHOTO_ALBUMS, ids.toSet()) }
    }

    fun loadCustomSharedAlbumUrls(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_SHARED_ALBUM_URLS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i).trim()
                    if (u.isNotBlank()) add(u)
                }
            }
        } catch (_: Exception) {
            emptyList()
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
        return null
    }

    fun savePhotoYearFilters(years: Set<Int>?) {
        prefs.edit {
            when {
                years == null -> remove(KEY_PHOTO_YEAR_FILTERS)
                years.isEmpty() -> putString(KEY_PHOTO_YEAR_FILTERS, "NONE")
                else -> putString(KEY_PHOTO_YEAR_FILTERS, years.sorted().joinToString(","))
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

    companion object {
        const val DEFAULT_SLIDE_INTERVAL_SEC = 10
        const val DEFAULT_DIM_BRIGHTNESS = 0.16f
        const val DEFAULT_GLOBAL_BRIGHTNESS = 0.85f

        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id_v1"
        private const val KEY_OAUTH_CLIENT_SECRET = "oauth_client_secret_v1"
        private const val KEY_PENDING_CODE_VERIFIER = "pending_code_verifier_v1"
        private const val KEY_PENDING_OAUTH_STATE = "pending_oauth_state_v1"
        private const val KEY_PENDING_REDIRECT_URI = "pending_redirect_uri_v1"
        private const val KEY_GOOGLE_TOKENS = "google_tokens_v1"
        private const val KEY_ACCOUNT_PROFILE = "google_account_profile_v1"
        private const val KEY_SELECTED_CALENDARS = "selected_calendar_ids_v1"
        private const val KEY_SELECTED_TASK_LISTS = "selected_task_list_ids_v1"
        private const val KEY_SHOPPING_TASK_LISTS = "shopping_task_list_ids_v1"
        private const val KEY_SELECTED_PHOTO_ALBUMS = "selected_photo_album_ids_v1"
        private const val KEY_CUSTOM_SHARED_ALBUM_URLS = "custom_shared_album_urls_v1"
        private const val KEY_USER_LOCATION = "user_location_v1"

        private const val KEY_SLIDE_INTERVAL_SEC = "local_slide_interval_sec_v1"
        private const val KEY_PHOTO_YEAR_FILTERS = "local_photo_years_v2"
        private const val KEY_PHOTO_ALBUM_FILTER = "local_photo_album_v1"
        private const val KEY_DIM_BRIGHTNESS = "local_dim_brightness_v1"
        private const val KEY_GLOBAL_BRIGHTNESS = "global_brightness_v1"
        private const val KEY_WAKE_ON_EVENT_REMINDER = "wake_on_event_reminder_v1"
        private const val KEY_CALENDAR_VIEW_MODE = "calendar_view_mode_v1"
        private const val KEY_CALENDAR_PRIMARY_MODE = "calendar_primary_mode_v1"
        private const val KEY_REMINDERS = "reminders_v3_oauth"
        private const val KEY_STARRED_IDS = "starred_photo_ids_v1"
        private const val KEY_PENDING_REM_OPS = "pending_rem_ops_v2_oauth"
        private const val KEY_ACTIVE_TASK_LIST_FILTERS = "active_task_list_filters_v1"
    }
}
