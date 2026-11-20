package com.nernal.frame.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

data class PickerSessionInfo(
    val id: String,
    val pickerUri: String,
)

class NernalRepository(private val cacheDir: File? = null) {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // 24 MB cache protects 32-bit device heap while holding ~10-15 downsampled 1280x800 RGB_565 bitmaps.
    private val bitmapCache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val diskCacheDir: File? by lazy {
        cacheDir?.let { File(it, "photo_disk_cache").apply { mkdirs() } }
    }
    private val diskLock = Any()

    // 14 MB map/radar tile cache
    private val tileCache = object : LruCache<String, Bitmap>(14 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    // -------------------------------------------------------------------------
    // Location & Weather
    // -------------------------------------------------------------------------

    suspend fun detectLocationByIp(): UserLocation? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", USER_AGENT_BROWSER)
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            }
            if (!body.isNullOrBlank()) {
                val obj = JSONObject(body)
                val lat = obj.optDouble("latitude", Double.NaN)
                val lon = obj.optDouble("longitude", Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) {
                    val city = obj.optString("city")
                    val region = obj.optString("region_code").ifBlank { obj.optString("region") }
                    val label = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
                        .uppercase(Locale.US)
                        .ifBlank { "LOCAL WEATHER" }
                    val tz = obj.optString("timezone").ifBlank { "America/New_York" }
                    return@runCatching UserLocation(label, lat, lon, tz)
                }
            }
            null
        }.getOrNull() ?: runCatching {
            val req = Request.Builder().url("http://ip-api.com/json/").build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            } ?: return@runCatching null
            val obj = JSONObject(body)
            if (obj.optString("status") != "success") return@runCatching null
            val lat = obj.optDouble("lat", Double.NaN)
            val lon = obj.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return@runCatching null
            val city = obj.optString("city")
            val region = obj.optString("region")
            val label = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
                .uppercase(Locale.US)
                .ifBlank { "LOCAL WEATHER" }
            val tz = obj.optString("timezone").ifBlank { "America/New_York" }
            UserLocation(label, lat, lon, tz)
        }.getOrNull()
    }

    suspend fun searchLocations(query: String): List<UserLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val req = Request.Builder()
                .url("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json")
                .header("User-Agent", USER_AGENT_BROWSER)
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            } ?: return@runCatching emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val o = results.optJSONObject(i) ?: continue
                    val city = o.optString("name")
                    val admin1 = o.optString("admin1")
                    val country = o.optString("country_code")
                    val suffix = if (country.equals("US", ignoreCase = true) && admin1.isNotBlank()) {
                        admin1
                    } else {
                        listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
                    }
                    val display = listOf(city, suffix).filter { it.isNotBlank() }.joinToString(", ")
                        .uppercase(Locale.US)
                    add(
                        UserLocation(
                            name = display.ifBlank { trimmed.uppercase(Locale.US) },
                            latitude = o.getDouble("latitude"),
                            longitude = o.getDouble("longitude"),
                            timezone = o.optString("timezone", "America/New_York").ifBlank { "America/New_York" },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchWeather(location: UserLocation): Result<WeatherData> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        runCatching {
            val tzEncoded = URLEncoder.encode(location.timezone, "UTF-8")
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${location.latitude}&longitude=${location.longitude}" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch" +
                "&timezone=$tzEncoded"

            val weatherReq = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT_BROWSER)
                .build()

            val weatherJsonStr = httpClient.newCall(weatherReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Open-Meteo HTTP ${resp.code}")
                resp.body?.string() ?: error("Empty Open-Meteo response")
            }

            val alerts = runCatching { fetchNwsAlerts(location.latitude, location.longitude) }.getOrDefault(emptyList())
            val parsed = parseWeatherJson(weatherJsonStr, alerts)
            val elapsed = System.currentTimeMillis() - startMs
            Log.d("NernalPerf", "fetchWeather completed in ${elapsed}ms for ${location.name} (temp=${parsed.currentTempF}°F, alerts=${parsed.alerts.size})")
            parsed
        }
    }

    private fun fetchNwsAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val url = String.format(Locale.US, "https://api.weather.gov/alerts/active?point=%.4f,%.4f", lat, lon)
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "NernalGoogleDashboard/1.0")
            .header("Accept", "application/geo+json")
            .build()

        val body = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            resp.body?.string() ?: return emptyList()
        }

        val root = JSONObject(body)
        val features = root.optJSONArray("features") ?: return emptyList()
        return buildList {
            for (i in 0 until features.length()) {
                val props = features.optJSONObject(i)?.optJSONObject("properties") ?: continue
                add(
                    WeatherAlert(
                        event = props.optString("event", "Weather Advisory"),
                        headline = props.optString("headline", ""),
                        severity = props.optString("severity", "Unknown"),
                        description = props.optString("description", ""),
                    )
                )
            }
        }
    }

    suspend fun fetchRadarData(
        lat: Double = UserLocation.DEFAULT.latitude,
        lon: Double = UserLocation.DEFAULT.longitude,
    ): Result<RadarData> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        runCatching {
            val rainViewer = runCatching {
                val req = Request.Builder().url(RAINVIEWER_URL).build()
                val body = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("RainViewer HTTP ${resp.code}")
                    resp.body?.string() ?: error("Empty RainViewer response")
                }
                parseRainViewerJson(body)
            }.getOrNull()

            // Verify RainViewer tile CDN isn't rate-limiting (HTTP 429) at user's region; otherwise use NOAA/IEM NEXRAD.
            val sampleFrame = rainViewer?.frames?.lastOrNull()
            val result = if (rainViewer != null && sampleFrame != null) {
                val probeX = floor(lonToTileX(lon, 7)).toInt().coerceIn(0, (1 shl 7) - 1)
                val probeY = floor(latToTileY(lat, 7)).toInt().coerceIn(0, (1 shl 7) - 1)
                val probeUrl = radarTileUrl(rainViewer.host, sampleFrame.path, 7, probeX, probeY)
                val probeOk = runCatching {
                    val req = Request.Builder().url(probeUrl).header("User-Agent", USER_AGENT_BROWSER).build()
                    httpClient.newCall(req).execute().use { it.isSuccessful }
                }.getOrDefault(false)
                if (probeOk) {
                    val past = rainViewer.frames.filter { !it.isForecast }.takeLast(5)
                    val forecast = rainViewer.frames.filter { it.isForecast }.take(2)
                    rainViewer.copy(frames = past + forecast)
                } else {
                    buildIemNexradData()
                }
            } else {
                buildIemNexradData()
            }
            val elapsed = System.currentTimeMillis() - startMs
            Log.d("NernalPerf", "fetchRadarData completed in ${elapsed}ms (frames=${result.frames.size}, host=${result.host})")
            result
        }
    }

    suspend fun loadMapTile(url: String, needsAlpha: Boolean): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$url|$needsAlpha"
        tileCache.get(key)?.takeIf { !it.isRecycled }?.let { return@withContext it }

        val maxAttempts = if (needsAlpha) 1 else 2
        var bytes: ByteArray? = null
        for (attempt in 1..maxAttempts) {
            bytes = runCatching {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT_BROWSER)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.bytes()
                }
            }.getOrNull()
            if (bytes != null) break
            if (attempt < maxAttempts) delay(250L)
        }
        val payload = bytes ?: return@withContext null

        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = if (needsAlpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeByteArray(payload, 0, payload.size, opts) ?: return@withContext null
        tileCache.put(key, bmp)
        bmp
    }

    // -------------------------------------------------------------------------
    // Google Calendar API v3
    // -------------------------------------------------------------------------

    suspend fun fetchCalendarList(accessToken: String): Result<List<GoogleCalendarInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error(extractGoogleApiError(text, "Calendar list HTTP ${resp.code}"))
                text
            }
            parseCalendarListJson(body)
        }
    }

    suspend fun fetchCalendarEvents(
        accessToken: String,
        calendarIds: Set<String>,
        zoneId: ZoneId,
        calendarColors: Map<String, String> = emptyMap(),
    ): List<ReminderItem> = withContext(Dispatchers.IO) {
        if (calendarIds.isEmpty()) return@withContext emptyList()
        val startMs = System.currentTimeMillis()
        val startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
        val endWindow = LocalDate.now(zoneId).plusDays(60).atStartOfDay(zoneId).toInstant()
        val timeMin = URLEncoder.encode(DateTimeFormatter.ISO_INSTANT.format(startOfToday), "UTF-8")
        val timeMax = URLEncoder.encode(DateTimeFormatter.ISO_INSTANT.format(endWindow), "UTF-8")

        val perCalendar = coroutineScope {
            calendarIds.map { calId ->
                async {
                    runCatching {
                        val encCalId = URLEncoder.encode(calId, "UTF-8")
                        val out = mutableListOf<ReminderItem>()
                        var pageToken: String? = null
                        var pages = 0
                        do {
                            val pageParam = if (pageToken.isNullOrBlank()) "" else "&pageToken=${URLEncoder.encode(pageToken, "UTF-8")}"
                            val url = "https://www.googleapis.com/calendar/v3/calendars/$encCalId/events" +
                                "?singleEvents=true&orderBy=startTime&timeMin=$timeMin&timeMax=$timeMax&maxResults=250$pageParam"
                            val req = Request.Builder()
                                .url(url)
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                            val body = httpClient.newCall(req).execute().use { resp ->
                                if (!resp.isSuccessful) return@use null
                                resp.body?.string()
                            } ?: break
                            out.addAll(parseCalendarEventsJson(body, calId, zoneId, calendarColors[calId]))
                            pageToken = runCatching {
                                JSONObject(body).optString("nextPageToken").takeIf { it.isNotBlank() }
                            }.getOrNull()
                            pages++
                        } while (!pageToken.isNullOrBlank() && pages < 5)
                        out
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
        }
        val result = perCalendar.flatten()
        val elapsed = System.currentTimeMillis() - startMs
        Log.d("NernalPerf", "fetchCalendarEvents: fetched ${result.size} events across ${calendarIds.size} calendars in ${elapsed}ms")
        result
    }

    suspend fun createCalendarEvent(
        accessToken: String,
        calendarId: String,
        text: String,
        dateKey: String?,
        zoneId: ZoneId,
    ): RemoteMutationResult<ReminderItem> = withContext(Dispatchers.IO) {
        try {
            val targetDate = runCatching {
                dateKey?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            }.getOrNull() ?: LocalDate.now(zoneId)
            val endDate = targetDate.plusDays(1)

            val payload = JSONObject().apply {
                put("summary", text)
                put("start", JSONObject().apply { put("date", targetDate.toString()) })
                put("end", JSONObject().apply { put("date", endDate.toString()) })
            }
            val encCalId = URLEncoder.encode(calendarId, "UTF-8")
            val req = Request.Builder()
                .url("https://www.googleapis.com/calendar/v3/calendars/$encCalId/events")
                .header("Authorization", "Bearer $accessToken")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext classifyHttpFailure(resp.code, body, "Calendar create failed")
                }
                val created = JSONObject(body)
                val remoteId = created.optString("id").takeIf { it.isNotBlank() }
                    ?: return@withContext RemoteMutationResult.PermanentError(200, "Missing created event id")
                RemoteMutationResult.Success(
                    ReminderItem(
                        id = "cal:$calendarId:$remoteId",
                        text = created.optString("summary", text),
                        category = ReminderCategory.EVENT,
                        dateKey = targetDate.toString(),
                        time = null,
                        completed = false,
                        createdAtMs = System.currentTimeMillis(),
                        sourceId = calendarId,
                        remoteId = remoteId,
                    )
                )
            }
        } catch (e: IOException) {
            RemoteMutationResult.TransientError(e.message ?: "Network error")
        } catch (e: Exception) {
            RemoteMutationResult.PermanentError(0, e.message ?: "Calendar create failed")
        }
    }

    suspend fun deleteCalendarEvent(
        accessToken: String,
        calendarId: String,
        eventId: String,
    ): RemoteMutationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val encCalId = URLEncoder.encode(calendarId, "UTF-8")
            val encEventId = URLEncoder.encode(eventId, "UTF-8")
            val req = Request.Builder()
                .url("https://www.googleapis.com/calendar/v3/calendars/$encCalId/events/$encEventId")
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 404 || resp.code == 410) {
                    RemoteMutationResult.Success(Unit)
                } else {
                    classifyHttpFailure(resp.code, resp.body?.string().orEmpty(), "Calendar delete failed")
                }
            }
        } catch (e: IOException) {
            RemoteMutationResult.TransientError(e.message ?: "Network error")
        } catch (e: Exception) {
            RemoteMutationResult.PermanentError(0, e.message ?: "Calendar delete failed")
        }
    }

    // -------------------------------------------------------------------------
    // Google Tasks API v1
    // -------------------------------------------------------------------------

    suspend fun fetchTaskLists(accessToken: String): Result<List<GoogleTaskListInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://tasks.googleapis.com/tasks/v1/users/@me/lists?maxResults=100")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error(extractGoogleApiError(text, "Task lists HTTP ${resp.code}"))
                text
            }
            parseTaskListsJson(body)
        }
    }

    suspend fun createTaskList(accessToken: String, title: String): Result<GoogleTaskListInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply { put("title", title.trim()) }
            val req = Request.Builder()
                .url("https://tasks.googleapis.com/tasks/v1/users/@me/lists")
                .header("Authorization", "Bearer $accessToken")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error(extractGoogleApiError(text, "Create task list HTTP ${resp.code}"))
                text
            }
            val obj = JSONObject(body)
            GoogleTaskListInfo(
                id = obj.getString("id"),
                title = obj.optString("title", title.trim()),
            )
        }
    }

    suspend fun fetchTasksForLists(
        accessToken: String,
        taskListIds: Set<String>,
    ): List<ReminderItem> = withContext(Dispatchers.IO) {
        if (taskListIds.isEmpty()) return@withContext emptyList()
        val startMs = System.currentTimeMillis()
        val perList = coroutineScope {
            taskListIds.map { listId ->
                async {
                    runCatching {
                        val encId = URLEncoder.encode(listId, "UTF-8")
                        val cat = ReminderCategory.TASK
                        val out = mutableListOf<ReminderItem>()
                        var pageToken: String? = null
                        var pages = 0
                        do {
                            val pageParam = if (pageToken.isNullOrBlank()) "" else "&pageToken=${URLEncoder.encode(pageToken, "UTF-8")}"
                            val url = "https://tasks.googleapis.com/tasks/v1/lists/$encId/tasks" +
                                "?showCompleted=true&showHidden=true&maxResults=100$pageParam"
                            val req = Request.Builder()
                                .url(url)
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                            val body = httpClient.newCall(req).execute().use { resp ->
                                if (!resp.isSuccessful) return@use null
                                resp.body?.string()
                            } ?: break
                            out.addAll(parseTasksListJson(body, listId, cat))
                            pageToken = runCatching {
                                JSONObject(body).optString("nextPageToken").takeIf { it.isNotBlank() }
                            }.getOrNull()
                            pages++
                        } while (!pageToken.isNullOrBlank() && pages < 5)
                        out
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
        }
        val result = perList.flatten()
        val elapsed = System.currentTimeMillis() - startMs
        Log.d("NernalPerf", "fetchTasksForLists: fetched ${result.size} tasks across ${taskListIds.size} lists in ${elapsed}ms")
        result
    }

    suspend fun createTask(
        accessToken: String,
        taskListId: String,
        title: String,
        category: ReminderCategory,
    ): RemoteMutationResult<ReminderItem> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply { put("title", title) }
            val encId = URLEncoder.encode(taskListId, "UTF-8")
            val req = Request.Builder()
                .url("https://tasks.googleapis.com/tasks/v1/lists/$encId/tasks")
                .header("Authorization", "Bearer $accessToken")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext classifyHttpFailure(resp.code, body, "Task create failed")
                }
                val obj = JSONObject(body)
                val remoteId = obj.optString("id").takeIf { it.isNotBlank() }
                    ?: return@withContext RemoteMutationResult.PermanentError(200, "Missing created task id")
                RemoteMutationResult.Success(
                    ReminderItem(
                        id = "task:$taskListId:$remoteId",
                        text = obj.optString("title", title),
                        category = category,
                        dateKey = obj.optString("due").take(10).takeIf { it.length == 10 },
                        completed = obj.optString("status") == "completed",
                        createdAtMs = System.currentTimeMillis(),
                        sourceId = taskListId,
                        remoteId = remoteId,
                    )
                )
            }
        } catch (e: IOException) {
            RemoteMutationResult.TransientError(e.message ?: "Network error")
        } catch (e: Exception) {
            RemoteMutationResult.PermanentError(0, e.message ?: "Task create failed")
        }
    }

    suspend fun updateTaskCompletion(
        accessToken: String,
        taskListId: String,
        taskId: String,
        completed: Boolean,
    ): RemoteMutationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("status", if (completed) "completed" else "needsAction")
                if (!completed) put("completed", JSONObject.NULL)
            }
            val encList = URLEncoder.encode(taskListId, "UTF-8")
            val encTask = URLEncoder.encode(taskId, "UTF-8")
            val req = Request.Builder()
                .url("https://tasks.googleapis.com/tasks/v1/lists/$encList/tasks/$encTask")
                .header("Authorization", "Bearer $accessToken")
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 404) {
                    RemoteMutationResult.Success(Unit)
                } else {
                    classifyHttpFailure(resp.code, resp.body?.string().orEmpty(), "Task update failed")
                }
            }
        } catch (e: IOException) {
            RemoteMutationResult.TransientError(e.message ?: "Network error")
        } catch (e: Exception) {
            RemoteMutationResult.PermanentError(0, e.message ?: "Task update failed")
        }
    }

    suspend fun deleteTask(
        accessToken: String,
        taskListId: String,
        taskId: String,
    ): RemoteMutationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val encList = URLEncoder.encode(taskListId, "UTF-8")
            val encTask = URLEncoder.encode(taskId, "UTF-8")
            val req = Request.Builder()
                .url("https://tasks.googleapis.com/tasks/v1/lists/$encList/tasks/$encTask")
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 404 || resp.code == 410) {
                    RemoteMutationResult.Success(Unit)
                } else {
                    classifyHttpFailure(resp.code, resp.body?.string().orEmpty(), "Task delete failed")
                }
            }
        } catch (e: IOException) {
            RemoteMutationResult.TransientError(e.message ?: "Network error")
        } catch (e: Exception) {
            RemoteMutationResult.PermanentError(0, e.message ?: "Task delete failed")
        }
    }

    private fun classifyHttpFailure(code: Int, errBody: String, fallback: String): RemoteMutationResult<Nothing> =
        if (code == 401 || code == 429 || code >= 500) {
            RemoteMutationResult.TransientError(extractGoogleApiError(errBody, "$fallback (HTTP $code)"))
        } else {
            RemoteMutationResult.PermanentError(code, extractGoogleApiError(errBody, "$fallback (HTTP $code)"))
        }

    // -------------------------------------------------------------------------
    // Google Photos (Picker API + Shared URL Scraper)
    // -------------------------------------------------------------------------

    suspend fun fetchSharedAlbumPhotos(
        customSharedUrls: List<String>,
        onInitialBatch: (List<AlbumPhoto>) -> Unit,
    ): Result<List<AlbumPhoto>> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        runCatching {
            val collected = ArrayList<AlbumPhoto>()
            val seen = HashSet<String>()

            if (customSharedUrls.isNotEmpty()) {
                val scrapedFirstPages = coroutineScope {
                    customSharedUrls.map { url ->
                        async { runCatching { fetchAlbumFirstPage(url) }.getOrNull() }
                    }.awaitAll().filterNotNull()
                }

                if (scrapedFirstPages.isNotEmpty()) {
                    val stage1 = mutableListOf<AlbumPhoto>()
                    val seen1 = HashSet<String>()
                    for (page in scrapedFirstPages) {
                        for (p in page.initialPhotos) {
                            if (seen1.add(p.id)) stage1.add(p)
                        }
                    }
                    if (stage1.isNotEmpty()) {
                        onInitialBatch(stage1)
                    }
                }

                val allLists = coroutineScope {
                    scrapedFirstPages.map { first ->
                        async { paginateRemainingAlbumPages(first, maxPages = 40) }
                    }.awaitAll()
                }
                for (list in allLists) {
                    for (p in list) {
                        if (seen.add(p.id)) collected.add(p)
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startMs
            Log.d("NernalPerf", "fetchSharedAlbumPhotos: total ${collected.size} photos fetched in ${elapsed}ms")
            collected
        }
    }

    // --- Google Photos Picker API ---

    suspend fun createPickerSession(accessToken: String): Result<PickerSessionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://photospicker.googleapis.com/v1/sessions")
                .header("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error(extractGoogleApiError(text, "Picker session HTTP ${resp.code}"))
                text
            }
            val root = JSONObject(body)
            val id = root.getString("id")
            val rawUri = root.getString("pickerUri")
            val autoCloseUri = if (rawUri.endsWith("/")) "${rawUri}autoclose" else "$rawUri/autoclose"
            PickerSessionInfo(id = id, pickerUri = autoCloseUri)
        }
    }

    suspend fun pollPickerSession(accessToken: String, sessionId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://photospicker.googleapis.com/v1/sessions/$sessionId")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            } ?: return@runCatching false
            JSONObject(body).optBoolean("mediaItemsSet", false)
        }.getOrDefault(false)
    }

    suspend fun fetchPickedMediaItems(accessToken: String, sessionId: String): Result<List<AlbumPhoto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = mutableListOf<AlbumPhoto>()
                var pageToken: String? = null
                var pages = 0
                do {
                    val url = buildString {
                        append("https://photospicker.googleapis.com/v1/mediaItems?sessionId=")
                        append(URLEncoder.encode(sessionId, "UTF-8"))
                        append("&pageSize=100")
                        if (!pageToken.isNullOrBlank()) {
                            append("&pageToken=")
                            append(URLEncoder.encode(pageToken, "UTF-8"))
                        }
                    }
                    val req = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val body = httpClient.newCall(req).execute().use { resp ->
                        val text = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) error(extractGoogleApiError(text, "Picker items HTTP ${resp.code}"))
                        text
                    }
                    val root = JSONObject(body)
                    val arr = root.optJSONArray("mediaItems") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val type = item.optString("type", "PHOTO")
                        if (type != "PHOTO" && type != "TYPE_UNSPECIFIED") continue
                        val mediaFile = item.optJSONObject("mediaFile") ?: continue
                        val baseUrl = mediaFile.optString("baseUrl")
                        if (!baseUrl.startsWith("http")) continue
                        val meta = mediaFile.optJSONObject("mediaFileMetadata")
                        val w = meta?.optInt("width", 1920)?.coerceAtLeast(1) ?: 1920
                        val h = meta?.optInt("height", 1080)?.coerceAtLeast(1) ?: 1080
                        val createTime = item.optString("createTime").takeIf { it.isNotBlank() }
                        val tsMs = createTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                        val yr = tsMs?.let {
                            runCatching { Instant.ofEpochMilli(it).atZone(EASTERN_ZONE).year }.getOrNull()?.takeIf { y -> y > 1970 }
                        }
                        out.add(
                            AlbumPhoto(
                                id = item.optString("id").ifBlank { "picker-${out.size}" },
                                url = baseUrl,
                                width = w,
                                height = h,
                                timestampMs = tsMs,
                                year = yr,
                                albumTitle = "Picked Photos",
                                albumUrl = PICKER_ALBUM_MARKER,
                            )
                        )
                    }
                    pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
                    pages++
                } while (!pageToken.isNullOrBlank() && pages < 10)
                out
            }
        }

    // --- Shared Google Photos Link Scraper (for user-supplied public album links) ---

    private data class AlbumPageSeed(
        val albumUrl: String,
        val albumTitle: String,
        val initialPhotos: List<AlbumPhoto>,
        val nextToken: String?,
        val shareId: String?,
        val albumKey: String?,
    )

    private fun fetchAlbumFirstPage(url: String): AlbumPageSeed {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT_BROWSER)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val html = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string() ?: error("Empty HTML for $url")
        }

        val albumTitle = extractAlbumTitle(html)
        val (initialArray, nextToken) = extractInitialItemsArray(html)
        val rawItems = mutableListOf<JSONArray>()
        appendItemsFromJsonArray(initialArray, rawItems)
        val initialPhotos = parseItemsToPhotos(rawItems, albumTitle, url)

        val mReq = SNACKC_REQ_REGEX.find(html)
        return AlbumPageSeed(
            albumUrl = url,
            albumTitle = albumTitle,
            initialPhotos = initialPhotos,
            nextToken = nextToken,
            shareId = mReq?.groupValues?.getOrNull(1),
            albumKey = mReq?.groupValues?.getOrNull(2),
        )
    }

    private fun paginateRemainingAlbumPages(seed: AlbumPageSeed, maxPages: Int): List<AlbumPhoto> {
        val allPhotos = seed.initialPhotos.toMutableList()
        var nextToken = seed.nextToken
        val shareId = seed.shareId
        val albumKey = seed.albumKey

        var page = 1
        while (!nextToken.isNullOrBlank() && !shareId.isNullOrBlank() && !albumKey.isNullOrBlank() && page < maxPages) {
            page++
            val (moreRaw, nextTok) = runCatching {
                fetchNextAlbumPage(shareId, nextToken!!, albumKey)
            }.getOrNull() ?: break
            if (moreRaw.isEmpty()) break
            allPhotos.addAll(parseItemsToPhotos(moreRaw, seed.albumTitle, seed.albumUrl))
            nextToken = nextTok
        }
        return allPhotos
    }

    private fun fetchNextAlbumPage(
        shareId: String,
        token: String,
        albumKey: String,
    ): Pair<List<JSONArray>, String?> {
        val payloadData = JSONArray().apply {
            put(shareId)
            put(token)
            put(JSONObject.NULL)
            put(albumKey)
        }
        val innerRpc = JSONArray().apply {
            put("snAcKc")
            put(payloadData.toString())
            put(JSONObject.NULL)
            put("generic")
        }
        val outer = JSONArray().apply {
            put(JSONArray().apply { put(innerRpc) })
        }

        val body = FormBody.Builder()
            .add("f.req", outer.toString())
            .build()

        val req = Request.Builder()
            .url("https://photos.google.com/_/PhotosUi/data/batchexecute")
            .post(body)
            .header("User-Agent", USER_AGENT_BROWSER)
            .build()

        val respText = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList<JSONArray>() to null
            resp.body?.string() ?: return emptyList<JSONArray>() to null
        }

        val out = mutableListOf<JSONArray>()
        var nextTok: String? = null
        for (line in respText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(")]}'")) continue
            runCatching {
                val parsedLine = JSONArray(trimmed)
                for (i in 0 until parsedLine.length()) {
                    val rpc = parsedLine.optJSONArray(i) ?: continue
                    if (rpc.length() > 2 && rpc.optString(1) == "snAcKc") {
                        val rawPayload = rpc.optString(2)
                        if (rawPayload.isNotEmpty()) {
                            val pageData = JSONArray(rawPayload)
                            appendItemsFromJsonArray(pageData.optJSONArray(1), out)
                            nextTok = pageData.optString(2).takeIf { it.isNotBlank() && it != "null" }
                        }
                    }
                }
            }
        }
        return out to nextTok
    }

    // -------------------------------------------------------------------------
    // Bitmap Loading
    // -------------------------------------------------------------------------

    suspend fun loadPhotoBitmap(
        photo: AlbumPhoto,
        accessToken: String? = null,
        maxWidth: Int = 1280,
        maxHeight: Int = 800,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val url = photo.displayUrl(maxWidth, maxHeight)
        bitmapCache.get(url)?.takeIf { !it.isRecycled }?.let { return@withContext it }

        readBitmapFromDiskCache(photo, maxWidth, maxHeight)?.let { cached ->
            bitmapCache.put(url, cached)
            return@withContext cached
        }

        val bytes = runCatching {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT_BROWSER)
            if (photo.albumUrl == PICKER_ALBUM_MARKER && !accessToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $accessToken")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.bytes()
            }
        }.getOrNull() ?: return@withContext null

        val decoded = decodeSampledBitmap(bytes, maxWidth, maxHeight) ?: return@withContext null
        bitmapCache.put(url, decoded)
        writeBitmapToDiskCache(photo, maxWidth, maxHeight, decoded)
        val elapsed = System.currentTimeMillis() - startMs
        Log.d("NernalPerf", "loadPhotoBitmap: loaded ${decoded.width}x${decoded.height} (${bytes.size / 1024} KB) in ${elapsed}ms for ${photo.albumTitle}")
        decoded
    }

    // -------------------------------------------------------------------------
    // User Feedback Relay
    // -------------------------------------------------------------------------

    suspend fun submitFeedback(
        email: String?,
        displayName: String?,
        appId: String,
        appVersion: String,
        device: String,
        comment: String,
        diagnostics: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("comment", comment.trim())
                if (!email.isNullOrBlank()) put("email", email.trim())
                if (!displayName.isNullOrBlank()) put("displayName", displayName.trim())
                put("appId", appId)
                put("appVersion", appVersion)
                put("device", device)
                if (!diagnostics.isNullOrBlank()) put("diagnostics", diagnostics)
            }
            val req = Request.Builder()
                .url(FEEDBACK_RELAY_URL)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(body).optString("error").takeIf { it.isNotBlank() }
                    }.getOrNull() ?: "Server returned HTTP ${resp.code}"
                    error(errMsg)
                }
                val id = runCatching { JSONObject(body).optString("id") }.getOrNull().orEmpty()
                id.ifBlank { "sent" }
            }
        }
    }

    fun getCachedBitmap(photo: AlbumPhoto): Bitmap? =
        bitmapCache.get(photo.displayUrl())?.takeIf { !it.isRecycled }

    private fun diskCacheFile(photo: AlbumPhoto, maxWidth: Int, maxHeight: Int): File? {
        val dir = diskCacheDir ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${photo.id}|${maxWidth}x${maxHeight}".toByteArray(Charsets.UTF_8))
        val hex = buildString(digest.size * 2) {
            for (b in digest) {
                val i = b.toInt() and 0xFF
                if (i < 16) append('0')
                append(i.toString(16))
            }
        }
        return File(dir, "$hex.jpg")
    }

    private fun readBitmapFromDiskCache(photo: AlbumPhoto, maxWidth: Int, maxHeight: Int): Bitmap? {
        val file = diskCacheFile(photo, maxWidth, maxHeight) ?: return null
        if (!file.exists() || file.length() <= 0L) return null
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        val bmp = runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }.getOrNull()
        if (bmp != null) {
            runCatching { file.setLastModified(System.currentTimeMillis()) }
        } else {
            runCatching { file.delete() }
        }
        return bmp
    }

    private fun writeBitmapToDiskCache(photo: AlbumPhoto, maxWidth: Int, maxHeight: Int, bitmap: Bitmap) {
        val file = diskCacheFile(photo, maxWidth, maxHeight) ?: return
        synchronized(diskLock) {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                FileOutputStream(tmp).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                if (!tmp.renameTo(file)) {
                    tmp.delete()
                }
                trimDiskCacheIfNeeded()
            }
        }
    }

    private fun trimDiskCacheIfNeeded() {
        val dir = diskCacheDir ?: return
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") } ?: return
        var totalBytes = files.sumOf { it.length() }
        if (totalBytes <= DISK_CACHE_MAX_BYTES) return
        val sortedOldest = files.sortedBy { it.lastModified() }
        for (f in sortedOldest) {
            if (totalBytes <= DISK_CACHE_TARGET_BYTES) break
            val len = f.length()
            if (f.delete()) {
                totalBytes -= len
            }
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, reqWidth, reqHeight)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    internal companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val PICKER_ALBUM_MARKER = "picker"
        const val FEEDBACK_RELAY_URL = "https://onnkiosk-feedback-relay-1041770212384.us-west1.run.app/api/feedback"
        private const val DISK_CACHE_MAX_BYTES = 180L * 1024 * 1024
        private const val DISK_CACHE_TARGET_BYTES = 150L * 1024 * 1024

        private val BEARER_TOKEN_REGEX = Regex("""(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{8,}""")
        private val JSON_SECRET_REGEX = Regex("""(?i)("(?:access_token|refresh_token|id_token|client_secret|oauthClientSecret)"\s*:\s*")([^"]+)(")""")
        private val QUERY_SECRET_REGEX = Regex("""(?i)((?:access_token|refresh_token|id_token|client_secret|code)=)([^&\s"']+)""")
        private val GOOGLE_SECRET_PREFIX_REGEX = Regex("""(?:GOCSPX-[A-Za-z0-9_-]+|ya29\.[A-Za-z0-9._-]+|1//[A-Za-z0-9._-]+)""")

        fun scrubSensitiveTokens(raw: String): String =
            raw
                .replace(BEARER_TOKEN_REGEX, "Bearer [REDACTED]")
                .replace(JSON_SECRET_REGEX, "$1[REDACTED]$3")
                .replace(QUERY_SECRET_REGEX, "$1[REDACTED]")
                .replace(GOOGLE_SECRET_PREFIX_REGEX, "[REDACTED]")

        val PHOTO_LINK_REGEX = Regex("""https://(?:photos\.app\.goo\.gl/[A-Za-z0-9_-]+|photos\.google\.com/(?:share|album)/[^\s"<>]+)""")

        val EASTERN_ZONE: ZoneId = ZoneId.of("America/New_York")

        fun lonToTileX(lon: Double, zoom: Int): Double =
            (lon + 180.0) / 360.0 * (1 shl zoom)

        fun latToTileY(lat: Double, zoom: Int): Double {
            val rad = Math.toRadians(lat)
            return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * (1 shl zoom)
        }

        const val RAINVIEWER_URL =
            "https://api.rainviewer.com/public/weather-maps.json"

        const val IEM_RADAR_HOST =
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0"

        private const val USER_AGENT_BROWSER =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        private val TIME_LABEL_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        fun extractGoogleApiError(body: String, fallback: String): String =
            runCatching {
                val err = JSONObject(body).optJSONObject("error")
                err?.optString("message")?.takeIf { it.isNotBlank() } ?: fallback
            }.getOrDefault(fallback)

        fun parseCalendarListJson(jsonText: String): List<GoogleCalendarInfo> {
            val items = JSONObject(jsonText).optJSONArray("items") ?: return emptyList()
            return buildList(items.length()) {
                for (i in 0 until items.length()) {
                    val o = items.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    if (id.isBlank()) continue
                    add(
                        GoogleCalendarInfo(
                            id = id,
                            summary = o.optString("summaryOverride").ifBlank { o.optString("summary", id) },
                            backgroundColor = o.optString("backgroundColor").takeIf { it.isNotBlank() },
                            primary = o.optBoolean("primary", false),
                            accessRole = o.optString("accessRole", "reader"),
                        )
                    )
                }
            }.sortedWith(compareByDescending<GoogleCalendarInfo> { it.primary }.thenBy { it.summary.lowercase(Locale.US) })
        }

        fun parseCalendarEventsJson(
            jsonText: String,
            calendarId: String,
            zoneId: ZoneId,
            calendarColor: String? = null,
        ): List<ReminderItem> {
            val items = JSONObject(jsonText).optJSONArray("items") ?: return emptyList()
            return buildList(items.length()) {
                for (i in 0 until items.length()) {
                    val o = items.optJSONObject(i) ?: continue
                    if (o.optString("status") == "cancelled") continue
                    val eventId = o.optString("id")
                    if (eventId.isBlank()) continue
                    val summary = o.optString("summary").ifBlank { "(No title)" }
                    val startObj = o.optJSONObject("start") ?: continue
                    val endObj = o.optJSONObject("end")
                    val desc = o.optString("description").trim().takeIf { it.isNotBlank() }

                    val colorId = o.optString("colorId").takeIf { it.isNotBlank() }
                    val eventColorHex = when (colorId) {
                        "1" -> "#7986CB" // Lavender
                        "2" -> "#33B679" // Sage
                        "3" -> "#8E24AA" // Grape
                        "4" -> "#E67C73" // Flamingo
                        "5" -> "#F6BF26" // Banana
                        "6" -> "#F4511E" // Tangerine
                        "7" -> "#039BE5" // Peacock
                        "8" -> "#616161" // Graphite
                        "9" -> "#3F51B5" // Blueberry
                        "10" -> "#0B8043" // Basil
                        "11" -> "#D50000" // Tomato
                        else -> null
                    } ?: calendarColor

                    val dateTimeStr = startObj.optString("dateTime").takeIf { it.isNotBlank() }
                    val dateOnlyStr = startObj.optString("date").takeIf { it.isNotBlank() }

                    if (dateTimeStr != null) {
                        var dateKey: String? = null
                        var timeStr: String? = null
                        var createdAtMs = System.currentTimeMillis()
                        runCatching {
                            val zdt = OffsetDateTime.parse(dateTimeStr).atZoneSameInstant(zoneId)
                            dateKey = zdt.toLocalDate().toString()
                            timeStr = TIME_LABEL_FMT.format(zdt)
                            createdAtMs = zdt.toInstant().toEpochMilli()
                        }
                        if (dateKey != null) {
                            add(
                                ReminderItem(
                                    id = "cal:$calendarId:$eventId",
                                    text = summary,
                                    category = ReminderCategory.EVENT,
                                    dateKey = dateKey,
                                    time = timeStr,
                                    completed = false,
                                    createdAtMs = createdAtMs,
                                    sourceId = calendarId,
                                    remoteId = eventId,
                                    notes = desc,
                                    color = eventColorHex,
                                )
                            )
                        }
                    } else if (dateOnlyStr != null) {
                        val startDate = runCatching { LocalDate.parse(dateOnlyStr.take(10)) }.getOrNull() ?: continue
                        val endExclusive = endObj?.optString("date")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                            ?.takeIf { it.isAfter(startDate) }
                            ?: startDate.plusDays(1)

                        val dayKey = startDate.toString()
                        val dayId = "cal:$calendarId:$eventId"
                        val dayMs = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                        val fullNotes = if (endExclusive.isAfter(startDate.plusDays(1))) {
                            val rangeNote = "Through ${endExclusive.minusDays(1)}"
                            if (desc != null) "$desc ($rangeNote)" else rangeNote
                        } else {
                            desc
                        }
                        add(
                            ReminderItem(
                                id = dayId,
                                text = summary,
                                category = ReminderCategory.EVENT,
                                dateKey = dayKey,
                                time = null,
                                completed = false,
                                createdAtMs = dayMs,
                                sourceId = calendarId,
                                remoteId = eventId,
                                notes = fullNotes,
                                color = eventColorHex,
                            )
                        )
                    }
                }
            }
        }

        fun parseTaskListsJson(jsonText: String): List<GoogleTaskListInfo> {
            val items = JSONObject(jsonText).optJSONArray("items") ?: return emptyList()
            return buildList(items.length()) {
                for (i in 0 until items.length()) {
                    val o = items.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val title = o.optString("title")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        add(GoogleTaskListInfo(id = id, title = title))
                    }
                }
            }
        }

        fun parseTasksListJson(
            jsonText: String,
            taskListId: String,
            category: ReminderCategory,
        ): List<ReminderItem> {
            val items = JSONObject(jsonText).optJSONArray("items") ?: return emptyList()
            return buildList(items.length()) {
                for (i in 0 until items.length()) {
                    val o = items.optJSONObject(i) ?: continue
                    if (o.optBoolean("deleted", false)) continue
                    val taskId = o.optString("id")
                    val title = o.optString("title").trim()
                    if (taskId.isBlank() || title.isBlank()) continue
                    val dueStr = o.optString("due").takeIf { it.length >= 10 }?.take(10)
                    val completed = o.optString("status") == "completed"
                    val updatedMs = o.optString("updated").takeIf { it.isNotBlank() }?.let {
                        runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
                    } ?: System.currentTimeMillis()
                    val notes = o.optString("notes").trim().takeIf { it.isNotBlank() }
                    add(
                        ReminderItem(
                            id = "task:$taskListId:$taskId",
                            text = title,
                            category = category,
                            dateKey = dueStr,
                            time = null,
                            completed = completed,
                            createdAtMs = updatedMs,
                            sourceId = taskListId,
                            remoteId = taskId,
                            notes = notes,
                        )
                    )
                }
            }
        }

        fun parsePhotoAlbumsJson(
            jsonText: String,
            arrayKey: String,
            isShared: Boolean,
        ): Pair<List<GooglePhotoAlbumInfo>, String?> {
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray(arrayKey)
            val albums = if (arr != null) {
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optString("id")
                        if (id.isBlank()) continue
                        add(
                            GooglePhotoAlbumInfo(
                                id = id,
                                title = o.optString("title").ifBlank { "Untitled Album" },
                                mediaItemsCount = o.optString("mediaItemsCount").toIntOrNull()
                                    ?: o.optInt("mediaItemsCount", 0),
                                coverPhotoBaseUrl = o.optString("coverPhotoBaseUrl").takeIf { it.isNotBlank() },
                                isShared = isShared,
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }
            val nextTok = root.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
            return albums to nextTok
        }

        fun parseMediaItemsJson(
            jsonText: String,
            albumTitle: String,
            albumId: String,
            zoneId: ZoneId = EASTERN_ZONE,
        ): Pair<List<AlbumPhoto>, String?> {
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("mediaItems")
            val photos = if (arr != null) {
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val mime = o.optString("mimeType", "")
                        if (mime.startsWith("video/")) continue
                        val baseUrl = o.optString("baseUrl")
                        if (!baseUrl.startsWith("http")) continue
                        val id = o.optString("id").ifBlank { "gphoto-$i" }
                        val meta = o.optJSONObject("mediaMetadata")
                        val w = meta?.optString("width")?.toIntOrNull() ?: meta?.optInt("width", 1920) ?: 1920
                        val h = meta?.optString("height")?.toIntOrNull() ?: meta?.optInt("height", 1080) ?: 1080
                        val creationTime = meta?.optString("creationTime")?.takeIf { it.isNotBlank() }
                        val tsMs = creationTime?.let {
                            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
                        }
                        val yr = tsMs?.let {
                            runCatching { Instant.ofEpochMilli(it).atZone(zoneId).year }.getOrNull()?.takeIf { y -> y > 1970 }
                        }
                        add(
                            AlbumPhoto(
                                id = id,
                                url = baseUrl,
                                width = w.coerceAtLeast(1),
                                height = h.coerceAtLeast(1),
                                timestampMs = tsMs,
                                year = yr,
                                albumTitle = albumTitle,
                                albumUrl = albumId,
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }
            val nextTok = root.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
            return photos to nextTok
        }

        fun buildIemNexradData(nowSec: Long = System.currentTimeMillis() / 1000L): RadarData {
            val baseSlot = (nowSec / 300L) * 300L
            val frames = (50 downTo 0 step 5).map { minsAgo ->
                val suffix = if (minsAgo == 0) "" else "-m${minsAgo.toString().padStart(2, '0')}m"
                RadarFrame(
                    timeSec = baseSlot - minsAgo * 60L,
                    path = "/nexrad-n0q-900913$suffix",
                    isForecast = false,
                )
            }
            return RadarData(host = IEM_RADAR_HOST, frames = frames, fetchedAtMs = nowSec * 1000L)
        }

        fun radarTileUrl(host: String, framePath: String, zoom: Int, tx: Int, ty: Int): String =
            if (framePath.startsWith("/nexrad-")) {
                "$host$framePath/$zoom/$tx/$ty.png"
            } else {
                "$host$framePath/256/$zoom/$tx/$ty/2/1_1.png"
            }

        private val OG_TITLE_REGEX = Regex("""<meta property="og:title" content="([^"]+)"""")
        private val TITLE_REGEX = Regex("""<title>([^<]+)</title>""")
        private val CALLBACK_REGEX =
            Regex("""AF_initDataCallback\((.*?)\);</script>""", setOf(RegexOption.DOT_MATCHES_ALL))
        private val DATA_SIDE_REGEX =
            Regex("""data:\s*(\[.*\])\s*,\s*sideChannel""", setOf(RegexOption.DOT_MATCHES_ALL))
        private val SNACKC_REQ_REGEX =
            Regex("""id:\s*['"]snAcKc['"][^}]*request:\s*\["([^"]+)",null,null,"([^"]+)"\]""")

        fun decodeHtmlEntities(input: String): String = input
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()

        fun extractAlbumTitle(html: String): String {
            val ogMatch = OG_TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)
            if (!ogMatch.isNullOrBlank()) {
                val primary = ogMatch.split(" · ").firstOrNull()?.trim().orEmpty()
                return decodeHtmlEntities(primary.ifEmpty { ogMatch })
            }
            val titleMatch = TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)
            if (!titleMatch.isNullOrBlank()) {
                val cleaned = titleMatch.replace(Regex("""\s*-\s*Google Photos.*$""", RegexOption.IGNORE_CASE), "")
                if (cleaned.isNotBlank()) return decodeHtmlEntities(cleaned)
            }
            return "Shared Album"
        }

        fun extractInitialItemsArray(html: String): Pair<JSONArray?, String?> {
            val callbacks = CALLBACK_REGEX.findAll(html).toList()
            for (cb in callbacks) {
                val dataMatch = DATA_SIDE_REGEX.find(cb.groupValues[1]) ?: continue
                val parsed = runCatching { JSONArray(dataMatch.groupValues[1]) }.getOrNull() ?: continue
                val candidate = parsed.optJSONArray(1) ?: continue
                if (candidate.length() > 0) {
                    val sample = candidate.optJSONArray(0)
                    val inner = sample?.optJSONArray(1)
                    val firstUrl = inner?.optString(0)
                    if (!firstUrl.isNullOrBlank() && firstUrl.startsWith("http")) {
                        val nextTok = parsed.optString(2).takeIf { it.isNotBlank() && it != "null" }
                        return candidate to nextTok
                    }
                }
            }
            return null to null
        }

        private fun appendItemsFromJsonArray(arr: JSONArray?, out: MutableList<JSONArray>) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                arr.optJSONArray(i)?.let(out::add)
            }
        }

        fun parseItemsToPhotos(
            rawItems: List<JSONArray>,
            albumTitle: String,
            albumUrl: String = "",
        ): List<AlbumPhoto> {
            return buildList {
                for (it in rawItems) {
                    val extras = it.optJSONObject(9)
                    if (extras != null && extras.has("76647426")) continue
                    val mediaInfo = it.optJSONArray(1) ?: continue
                    val photoUrl = mediaInfo.optString(0)
                    if (!photoUrl.startsWith("http")) continue

                    val rawTs = it.opt(2) as? Number
                    val tsMs = rawTs?.toLong()?.let { n -> if (n < 10_000_000_000L) n * 1000L else n }
                    val photoYear = tsMs?.takeIf { it > 0L }?.let { ms ->
                        runCatching {
                            Instant.ofEpochMilli(ms).atZone(EASTERN_ZONE).year
                        }.getOrNull()?.takeIf { it > 1970 }
                    }

                    val id = it.optString(0).takeIf { s -> s.isNotBlank() } ?: "photo-$size"
                    val w = mediaInfo.optInt(1, 1920).coerceAtLeast(1)
                    val h = mediaInfo.optInt(2, 1080).coerceAtLeast(1)
                    add(
                        AlbumPhoto(
                            id = id,
                            url = photoUrl,
                            width = w,
                            height = h,
                            timestampMs = tsMs,
                            year = photoYear,
                            albumTitle = albumTitle,
                            albumUrl = albumUrl,
                        )
                    )
                }
            }
        }

        fun filterPhotos(
            library: List<AlbumPhoto>,
            yearFilters: Set<Int>? = null,
            albumFilter: String? = null,
        ): List<AlbumPhoto> {
            val hasYearFilter = !yearFilters.isNullOrEmpty()
            val hasAlbumFilter = !albumFilter.isNullOrBlank()
            if (!hasYearFilter && !hasAlbumFilter) return library
            return library.filter { photo ->
                (!hasYearFilter || (photo.year != null && photo.year in yearFilters!!)) &&
                    (!hasAlbumFilter || photo.albumTitle == albumFilter)
            }
        }

        fun filterPhotos(
            library: List<AlbumPhoto>,
            yearFilter: Int?,
            albumFilter: String? = null,
        ): List<AlbumPhoto> = filterPhotos(library, yearFilter?.let { setOf(it) }, albumFilter)

        fun selectBalancedPhotos(
            library: List<AlbumPhoto>,
            maxCount: Int = 400,
            favorites: Set<String> = emptySet(),
        ): List<AlbumPhoto> {
            if (library.size <= maxCount) return library.shuffled()

            val favPhotos = library.filter { it.id in favorites }
            val nonFavs = library.filter { it.id !in favorites }
            val needed = (maxCount - favPhotos.size).coerceAtLeast(0)

            val queues = nonFavs
                .groupBy { it.albumUrl.ifBlank { it.albumTitle } }
                .values
                .map { it.shuffled().toMutableList() }
                .toMutableList()

            val selected = ArrayList<AlbumPhoto>(needed)
            var idx = 0
            while (selected.size < needed && queues.any { it.isNotEmpty() }) {
                val q = queues[idx % queues.size]
                if (q.isNotEmpty()) selected.add(q.removeAt(q.lastIndex))
                idx++
            }
            return (favPhotos + selected).shuffled()
        }

        fun parseRainViewerJson(body: String): RadarData {
            val root = JSONObject(body)
            val host = root.optString("host", "https://tilecache.rainviewer.com")
            val radar = root.optJSONObject("radar")
            val past = radar?.optJSONArray("past")
            val nowcast = radar?.optJSONArray("nowcast")
            val frames = buildList {
                past?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val path = o.optString("path")
                        if (path.isNotBlank()) add(RadarFrame(o.optLong("time"), path, isForecast = false))
                    }
                }
                nowcast?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val path = o.optString("path")
                        if (path.isNotBlank()) add(RadarFrame(o.optLong("time"), path, isForecast = true))
                    }
                }
            }
            return RadarData(host = host, frames = frames, fetchedAtMs = System.currentTimeMillis())
        }

        fun parseWeatherJson(jsonText: String, alerts: List<WeatherAlert>): WeatherData {
            val root = JSONObject(jsonText)
            val current = root.getJSONObject("current")
            val dailyObj = root.getJSONObject("daily")
            val times = dailyObj.getJSONArray("time")
            val codes = dailyObj.getJSONArray("weather_code")
            val maxTemps = dailyObj.getJSONArray("temperature_2m_max")
            val minTemps = dailyObj.getJSONArray("temperature_2m_min")
            val precipProbs = dailyObj.optJSONArray("precipitation_probability_max")
            val sunrises = dailyObj.optJSONArray("sunrise")
            val sunsets = dailyObj.optJSONArray("sunset")

            val dayCount = minOf(times.length(), 4)
            val daily = buildList(dayCount) {
                for (i in 0 until dayCount) {
                    val iso = times.getString(i)
                    val label = if (i == 0) {
                        "Today"
                    } else {
                        runCatching {
                            LocalDate.parse(iso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                        }.getOrDefault(iso.takeLast(5))
                    }
                    add(
                        DailyForecast(
                            dateIso = iso,
                            dayLabel = label,
                            weatherCode = codes.optInt(i, 0),
                            maxTempF = maxTemps.optDouble(i, 0.0).roundToInt(),
                            minTempF = minTemps.optDouble(i, 0.0).roundToInt(),
                            precipChance = precipProbs?.optInt(i, 0) ?: 0,
                        )
                    )
                }
            }

            val code = current.optInt("weather_code", 0)
            return WeatherData(
                currentTempF = current.optDouble("temperature_2m", 0.0).roundToInt(),
                apparentTempF = current.optDouble("apparent_temperature", 0.0).roundToInt(),
                humidityPercent = current.optInt("relative_humidity_2m", 0),
                windMph = current.optDouble("wind_speed_10m", 0.0).roundToInt(),
                weatherCode = code,
                isDay = current.optInt("is_day", 1) == 1,
                description = wmoDescription(code),
                precipInches = current.optDouble("precipitation", 0.0),
                sunriseIso = sunrises?.optString(0).orEmpty(),
                sunsetIso = sunsets?.optString(0).orEmpty(),
                daily = daily,
                alerts = alerts,
                fetchedAtMs = System.currentTimeMillis(),
            )
        }

        fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
            var inSampleSize = 1
            if (srcH > reqH || srcW > reqW) {
                val halfH = srcH / 2
                val halfW = srcW / 2
                while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }
    }
}
