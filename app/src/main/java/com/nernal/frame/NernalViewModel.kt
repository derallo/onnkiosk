package com.nernal.frame

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nernal.frame.data.AlbumPhoto
import com.nernal.frame.data.AppReleaseInfo
import com.nernal.frame.data.AppUpdateManager
import com.nernal.frame.data.CalendarViewMode
import com.nernal.frame.data.FramePrefs
import com.nernal.frame.data.GoogleAccountProfile
import com.nernal.frame.data.GoogleAuthManager
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.GoogleCalendarInfo
import com.nernal.frame.data.GoogleTaskListInfo
import com.nernal.frame.data.HubFilter
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.PendingReminderAction
import com.nernal.frame.data.PendingReminderOp
import com.nernal.frame.data.RadarData
import com.nernal.frame.data.ReminderCategory
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.RemoteMutationResult
import com.nernal.frame.data.TokenResult
import com.nernal.frame.data.UpdateCheckResult
import com.nernal.frame.data.UserLocation
import com.nernal.frame.data.WeatherData
import com.nernal.frame.data.isEventWithinReminderWindow
import com.nernal.frame.data.shouldAutoCloseEventAlert
import com.nernal.frame.data.sortReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class NernalUiState(
    val clockTime: String = "",
    val clockAmPm: String = "",
    val clockDate: String = "",
    val todayKey: String = "",
    val userLocation: UserLocation = UserLocation.DEFAULT,
    val locationSearchResults: List<UserLocation> = emptyList(),
    val locationSearching: Boolean = false,
    val weather: WeatherData? = null,
    val weatherLoading: Boolean = true,
    val weatherError: String? = null,
    val reminders: List<ReminderItem> = emptyList(),
    val hubFilter: HubFilter = HubFilter.ALL,
    val allPhotos: List<AlbumPhoto> = emptyList(),
    val libraryTotalCount: Int = 0,
    val starredIds: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
    val currentPhotoIndex: Int = 0,
    val currentBitmap: Bitmap? = null,
    val photoLoading: Boolean = false,
    val albumsSyncing: Boolean = false,
    val albumError: String? = null,
    val autoAdvance: Boolean = true,
    val slideIntervalSec: Int = FramePrefs.DEFAULT_SLIDE_INTERVAL_SEC,
    val selectedPhotoYears: Set<Int> = emptySet(),
    val photoAlbumFilter: String? = null,
    val availableYears: List<Int> = emptyList(),
    val availableAlbums: List<String> = emptyList(),
    val activeTaskListFilters: Set<String> = emptySet(),
    val showHelpDialog: Boolean = false,
    val dimBrightness: Float = FramePrefs.DEFAULT_DIM_BRIGHTNESS,
    val globalBrightness: Float = FramePrefs.DEFAULT_GLOBAL_BRIGHTNESS,
    val showSettingsDialog: Boolean = false,
    val fullscreenMode: Boolean = false,
    val isDimmed: Boolean = false,
    val showRadarModal: Boolean = false,
    val radarData: RadarData? = null,
    val radarLoading: Boolean = false,
    // Google OAuth & Sources
    val showAccountDialog: Boolean = false,
    val authState: GoogleAuthState = GoogleAuthState.NOT_SIGNED_IN,
    val accountProfile: GoogleAccountProfile? = null,
    val authError: String? = null,
    val oauthClientId: String = "",
    val oauthClientSecret: String = "",
    val activeAuthUrl: String? = null,
    val lanHelperUrl: String? = null,
    val showInAppAuthWebView: Boolean = false,
    val availableCalendars: List<GoogleCalendarInfo> = emptyList(),
    val selectedCalendarIds: Set<String> = emptySet(),
    val calendarError: String? = null,
    val availableTaskLists: List<GoogleTaskListInfo> = emptyList(),
    val selectedTaskListIds: Set<String> = emptySet(),
    val tasksError: String? = null,
    val customSharedAlbumUrls: List<String> = emptyList(),
    val photosApiError: String? = null,
    val pickerSessionUri: String? = null,
    val pickerPolling: Boolean = false,
    val googleSyncing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val lastGoogleSyncMs: Long = 0L,
    val syncNotice: String? = null,
    val wakeOnEventReminder: Boolean = true,
    val isAllYearsMode: Boolean = true,
    val activeEventAlert: ReminderItem? = null,
    val selectedEventDetail: ReminderItem? = null,
    val activePhotos: List<AlbumPhoto> = emptyList(),
    val recentlyDeletedReminder: ReminderItem? = null,
    val calendarViewMode: CalendarViewMode = CalendarViewMode.ITINERARY,
    val calendarPrimaryMode: Boolean = false,
    val updateRelease: AppReleaseInfo? = null,
    val isCheckingForUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val updateError: String? = null,
    val showUpdateDialog: Boolean = false,
    val updateNotice: String? = null,
    val showFeedbackDialog: Boolean = false,
    val isSubmittingFeedback: Boolean = false,
    val feedbackNotice: String? = null,
    val feedbackError: String? = null,
) {
    val currentPhoto: AlbumPhoto?
        get() {
            val list = activePhotos
            if (list.isEmpty()) return null
            return list[currentPhotoIndex.coerceIn(0, list.lastIndex)]
        }

    val isCurrentStarred: Boolean
        get() = currentPhoto?.id?.let { it in starredIds } == true

    val isAllYearsSelected: Boolean
        get() = isAllYearsMode || (availableYears.isNotEmpty() && selectedPhotoYears.size == availableYears.size && selectedPhotoYears.containsAll(availableYears))
}

class NernalViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = FramePrefs(application)
    private val repo = NernalRepository(application.cacheDir)
    private val authManager = GoogleAuthManager(prefs, repo.httpClient)

    private val _uiState = MutableStateFlow(NernalUiState())
    val uiState: StateFlow<NernalUiState> = _uiState.asStateFlow()

    private val syncMutex = Mutex()
    private var libraryPool: List<AlbumPhoto> = emptyList()
    private var lastManualNavMs: Long = 0L
    private var lastPhotosRefreshMs: Long = 0L
    private var pickerPollJob: Job? = null
    private var photoLoadJob: Job? = null
    private var undoDeleteJob: Job? = null
    private var pendingUndoDeleteItem: ReminderItem? = null
    private var consecutivePhotoFailures = 0

    init {
        val savedLocation = prefs.loadUserLocation()
        val tokens = prefs.loadGoogleTokens()
        val profile = prefs.loadAccountProfile()
        val remOps = prefs.loadPendingReminderOps()
        val initialAuthState = if (tokens != null) GoogleAuthState.SIGNED_IN else GoogleAuthState.NOT_SIGNED_IN
        val savedYears = prefs.loadPhotoYearFilters()
        val isAllYears = savedYears == null
        val selTaskLists = prefs.loadSelectedTaskListIds()
        val savedActiveTaskListFilters = prefs.loadActiveTaskListFilters() ?: selTaskLists

        _uiState.update {
            it.copy(
                userLocation = savedLocation ?: UserLocation.DEFAULT,
                reminders = sortReminders(prefs.loadReminders()),
                starredIds = prefs.loadStarredIds(),
                slideIntervalSec = prefs.loadSlideIntervalSec(),
                selectedPhotoYears = savedYears ?: emptySet(),
                isAllYearsMode = isAllYears,
                photoAlbumFilter = prefs.loadPhotoAlbumFilter(),
                dimBrightness = prefs.loadDimBrightness(),
                authState = initialAuthState,
                accountProfile = profile,
                oauthClientId = prefs.loadOAuthClientId(),
                oauthClientSecret = prefs.loadOAuthClientSecret(),
                selectedCalendarIds = prefs.loadSelectedCalendarIds(),
                selectedTaskListIds = selTaskLists,
                activeTaskListFilters = savedActiveTaskListFilters,
                customSharedAlbumUrls = prefs.loadCustomSharedAlbumUrls(),
                pendingSyncCount = remOps.size,
                wakeOnEventReminder = prefs.loadWakeOnEventReminder(),
                globalBrightness = prefs.loadGlobalBrightness(),
                calendarViewMode = prefs.loadCalendarViewMode(),
                calendarPrimaryMode = prefs.loadCalendarPrimaryMode(),
                hubFilter = if (prefs.loadCalendarPrimaryMode()) HubFilter.EVENTS else HubFilter.ALL,
            )
        }
        updateClock()

        startClockLoop()
        startWeatherLoop()
        startInitialLoadAndSyncLoops()
        startSlideshowLoop()
        startUpdateCheckLoop()
    }

    override fun onCleared() {
        super.onCleared()
        authManager.stopAuthServer()
        pickerPollJob?.cancel()
        photoLoadJob?.cancel()
        undoDeleteJob?.cancel()
    }

    private fun currentZoneId(): ZoneId =
        runCatching { ZoneId.of(_uiState.value.userLocation.timezone) }
            .getOrDefault(NernalRepository.EASTERN_ZONE)

    // -------------------------------------------------------------------------
    // Clock & Weather Loops
    // -------------------------------------------------------------------------

    private fun startClockLoop() {
        viewModelScope.launch {
            while (isActive) {
                val nowMs = System.currentTimeMillis()
                val delayMs = (60_000L - (nowMs % 60_000L)).coerceIn(500L, 60_000L)
                delay(delayMs)
                updateClock()
            }
        }
    }

    private val alertedEventKeys = mutableSetOf<String>()

    private fun updateClock() {
        val now = ZonedDateTime.now(currentZoneId())
        val timeStr = TIME_FMT.format(now)
        val amPmStr = AMPM_FMT.format(now).uppercase(Locale.US)
        val dateStr = DATE_FMT.format(now)
        val todayStr = DATE_KEY_FMT.format(now)
        val cur = _uiState.value
        if (cur.clockTime != timeStr || cur.clockAmPm != amPmStr || cur.clockDate != dateStr || cur.todayKey != todayStr) {
            _uiState.update {
                it.copy(
                    clockTime = timeStr,
                    clockAmPm = amPmStr,
                    clockDate = dateStr,
                    todayKey = todayStr,
                )
            }
        }
        checkUpcomingEventReminders(now)
    }

    private fun checkUpcomingEventReminders(now: ZonedDateTime = ZonedDateTime.now(currentZoneId())) {
        val state = _uiState.value
        val todayKey = DATE_KEY_FMT.format(now)
        val nowMinutes = now.hour * 60 + now.minute

        val currentAlert = state.activeEventAlert
        if (currentAlert != null && shouldAutoCloseEventAlert(
                alert = currentAlert,
                reminders = state.reminders,
                todayKey = todayKey,
                nowMinutes = nowMinutes,
                wakeEnabled = state.wakeOnEventReminder,
            )
        ) {
            _uiState.update { it.copy(activeEventAlert = null) }
        }

        if (!state.wakeOnEventReminder) return

        val upcomingEvent = state.reminders.firstOrNull { item ->
            isEventWithinReminderWindow(item, todayKey, nowMinutes) &&
                "${item.id}@${item.dateKey}" !in alertedEventKeys
        }

        if (upcomingEvent != null) {
            alertedEventKeys.add("${upcomingEvent.id}@${upcomingEvent.dateKey}")
            _uiState.update { it.copy(activeEventAlert = upcomingEvent) }
            if (_uiState.value.isDimmed) {
                setDimmed(false)
            }
        }
    }

    private fun startWeatherLoop() {
        viewModelScope.launch {
            while (isActive) {
                refreshWeather()
                delay(WEATHER_REFRESH_MS)
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val location = _uiState.value.userLocation
            _uiState.update { it.copy(weatherLoading = true, weatherError = null) }
            repo.fetchWeather(location)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(weather = data, weatherLoading = false, weatherError = null)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            weatherLoading = false,
                            weatherError = err.message ?: "Weather unavailable",
                        )
                    }
                }
        }
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _uiState.update { it.copy(locationSearchResults = emptyList(), locationSearching = false) }
                return@launch
            }
            _uiState.update { it.copy(locationSearching = true) }
            val results = repo.searchLocations(query)
            _uiState.update { it.copy(locationSearchResults = results, locationSearching = false) }
        }
    }

    fun selectUserLocation(location: UserLocation) {
        prefs.saveUserLocation(location)
        _uiState.update {
            it.copy(
                userLocation = location,
                locationSearchResults = emptyList(),
                syncNotice = "Location set to ${location.name}",
            )
        }
        updateClock()
        refreshWeather()
    }

    fun autoDetectLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(locationSearching = true) }
            val detected = repo.detectLocationByIp()
            if (detected != null) {
                prefs.saveUserLocation(detected)
                _uiState.update {
                    it.copy(
                        userLocation = detected,
                        locationSearching = false,
                        locationSearchResults = emptyList(),
                        syncNotice = "Detected location: ${detected.name}",
                    )
                }
                updateClock()
                refreshWeather()
            } else {
                _uiState.update {
                    it.copy(
                        locationSearching = false,
                        syncNotice = "Unable to detect location automatically. Search by city below.",
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Google OAuth 2.0 Sign-In & Config
    // -------------------------------------------------------------------------

    fun updateOAuthClientConfig(clientId: String, clientSecret: String) {
        prefs.saveOAuthClientId(clientId)
        prefs.saveOAuthClientSecret(clientSecret)
        _uiState.update {
            it.copy(
                oauthClientId = prefs.loadOAuthClientId(),
                oauthClientSecret = prefs.loadOAuthClientSecret(),
                authError = null,
                syncNotice = "Saved OAuth Client configuration",
            )
        }
    }

    fun startGoogleSignIn(openInAppWebView: Boolean = false) {
        val result = authManager.startAuthSession(viewModelScope) { callbackUrl ->
            viewModelScope.launch {
                completeGoogleSignIn(callbackUrl)
            }
        }
        result.onSuccess { session ->
            _uiState.update {
                it.copy(
                    authState = GoogleAuthState.SIGNING_IN,
                    authError = null,
                    activeAuthUrl = session.authUrl,
                    lanHelperUrl = session.lanHelperUrl,
                    showInAppAuthWebView = openInAppWebView,
                )
            }
            if (!openInAppWebView) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, session.authUrl.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            showInAppAuthWebView = true,
                            syncNotice = "No browser app found — using in-app sign-in",
                        )
                    }
                }
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    authState = GoogleAuthState.ERROR,
                    authError = err.message ?: "Unable to start Google Sign-In",
                    showInAppAuthWebView = false,
                )
            }
        }
    }

    fun dismissInAppAuthWebView() {
        _uiState.update { it.copy(showInAppAuthWebView = false) }
    }

    fun completeGoogleSignIn(rawCodeOrUrl: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    authState = GoogleAuthState.SIGNING_IN,
                    authError = null,
                    showInAppAuthWebView = false,
                )
            }
            authManager.exchangeCodeForTokens(rawCodeOrUrl)
                .onSuccess { (_, profile) ->
                    _uiState.update {
                        it.copy(
                            authState = GoogleAuthState.SIGNED_IN,
                            accountProfile = profile,
                            authError = null,
                            activeAuthUrl = null,
                            lanHelperUrl = null,
                            syncNotice = "Signed in as ${profile.email}",
                        )
                    }
                    refreshGoogleMetadataAndSync(forcePhotoRefresh = true)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            authState = GoogleAuthState.ERROR,
                            authError = err.message ?: "Google sign-in failed",
                        )
                    }
                }
        }
    }

    fun signOutGoogle() {
        authManager.stopAuthServer()
        pickerPollJob?.cancel()
        photoLoadJob?.cancel()
        undoDeleteJob?.cancel()
        pendingUndoDeleteItem = null
        prefs.clearGoogleAuth()
        libraryPool = emptyList()
        lastPhotosRefreshMs = 0L
        _uiState.update {
            it.copy(
                authState = GoogleAuthState.NOT_SIGNED_IN,
                accountProfile = null,
                authError = null,
                activeAuthUrl = null,
                lanHelperUrl = null,
                showInAppAuthWebView = false,
                availableCalendars = emptyList(),
                selectedCalendarIds = emptySet(),
                calendarError = null,
                availableTaskLists = emptyList(),
                selectedTaskListIds = emptySet(),
                tasksError = null,
                photosApiError = null,
                pickerSessionUri = null,
                pickerPolling = false,
                reminders = emptyList(),
                allPhotos = emptyList(),
                activePhotos = emptyList(),
                libraryTotalCount = 0,
                currentBitmap = null,
                currentPhotoIndex = 0,
                pendingSyncCount = 0,
                recentlyDeletedReminder = null,
                activeEventAlert = null,
                syncNotice = "Signed out of Google",
            )
        }
    }

    // -------------------------------------------------------------------------
    // Google Sync & Source Selection
    // -------------------------------------------------------------------------

    private fun startInitialLoadAndSyncLoops() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { prefs.loadAlbumLibrary() }
            if (cached.photos.isNotEmpty()) {
                libraryPool = cached.photos
                applyLibraryPoolToState(cached.totalLibraryCount, syncDone = true)
            }

            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                // Force fresh photo URLs on startup since Google Photos baseUrl expires after 60m
                refreshGoogleMetadataAndSync(forcePhotoRefresh = true)
            } else if (_uiState.value.customSharedAlbumUrls.isNotEmpty() && cached.photos.isEmpty()) {
                refreshPhotoAlbumsOnly()
            }

            while (isActive) {
                val interval = if (_uiState.value.isDimmed) GOOGLE_SYNC_DIMMED_INTERVAL_MS else GOOGLE_SYNC_INTERVAL_MS
                delay(interval)
                if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                    val photosExpired = System.currentTimeMillis() - lastPhotosRefreshMs >= PHOTOS_URL_REFRESH_MS
                    syncGoogleData(includePhotos = photosExpired)
                }
            }
        }
    }

    fun syncGoogleNow() {
        viewModelScope.launch {
            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                refreshGoogleMetadataAndSync(forcePhotoRefresh = true)
            } else if (_uiState.value.customSharedAlbumUrls.isNotEmpty()) {
                refreshPhotoAlbumsOnly()
            }
        }
    }

    private suspend fun resolveAccessTokenForSync(): String? =
        when (val res = authManager.getAccessTokenResult()) {
            is TokenResult.Valid -> res.accessToken
            is TokenResult.AuthRevoked -> {
                _uiState.update {
                    it.copy(
                        authState = GoogleAuthState.ERROR,
                        authError = res.message,
                        googleSyncing = false,
                    )
                }
                null
            }
            is TokenResult.TransientFailure -> {
                _uiState.update {
                    it.copy(
                        googleSyncing = false,
                        syncNotice = "Offline or temporary Google auth issue — retrying next cycle",
                    )
                }
                null
            }
        }

    private suspend fun refreshGoogleMetadataAndSync(forcePhotoRefresh: Boolean) = syncMutex.withLock {
        val token = resolveAccessTokenForSync() ?: return@withLock

        _uiState.update { it.copy(googleSyncing = true) }

        val (profileRes, calRes, taskListRes) = coroutineScope {
            val pDef = async {
                if (_uiState.value.accountProfile == null) {
                    runCatching { authManager.fetchUserProfile(token) }.getOrNull()
                } else null
            }
            val cDef = async { repo.fetchCalendarList(token) }
            val tDef = async { repo.fetchTaskLists(token) }
            MetadataBundle(pDef.await(), cDef.await(), tDef.await())
        }

        profileRes?.let { prof ->
            withContext(Dispatchers.IO) { prefs.saveAccountProfile(prof) }
            _uiState.update { it.copy(accountProfile = prof) }
        }

        calRes.onSuccess { cals ->
            var selCals = withContext(Dispatchers.IO) { prefs.loadSelectedCalendarIds() }
            if (!prefs.hasSelectedCalendarPreference() && cals.isNotEmpty()) {
                val primary = cals.filter { it.primary }.map { it.id }.toSet()
                selCals = primary.ifEmpty { setOf(cals.first().id) }
                withContext(Dispatchers.IO) { prefs.saveSelectedCalendarIds(selCals) }
            }
            _uiState.update {
                it.copy(availableCalendars = cals, selectedCalendarIds = selCals, calendarError = null)
            }
        }.onFailure { err ->
            _uiState.update { it.copy(calendarError = err.message) }
        }

        taskListRes.onSuccess { lists ->
            var selLists = withContext(Dispatchers.IO) { prefs.loadSelectedTaskListIds() }
            if (!prefs.hasSelectedTaskListPreference() && lists.isNotEmpty()) {
                selLists = lists.map { it.id }.toSet()
                withContext(Dispatchers.IO) {
                    prefs.saveSelectedTaskListIds(selLists)
                }
            }
            _uiState.update {
                it.copy(
                    availableTaskLists = lists,
                    selectedTaskListIds = selLists,
                    tasksError = null,
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(tasksError = err.message) }
        }

        syncGoogleDataLocked(includePhotos = forcePhotoRefresh, preloadedToken = token)
    }

    private data class MetadataBundle(
        val profile: GoogleAccountProfile?,
        val calendars: Result<List<GoogleCalendarInfo>>,
        val taskLists: Result<List<GoogleTaskListInfo>>,
    )

    private suspend fun syncGoogleData(includePhotos: Boolean) = syncMutex.withLock {
        syncGoogleDataLocked(includePhotos = includePhotos, preloadedToken = null)
    }

    private suspend fun syncGoogleDataLocked(includePhotos: Boolean, preloadedToken: String?) {
        val token = preloadedToken ?: resolveAccessTokenForSync() ?: return

        _uiState.update { it.copy(googleSyncing = true) }

        // 1. Flush any offline pending ops FIFO
        flushPendingReminderOps(token)

        // 2. Pull Calendar events + Google Tasks in parallel
        val stateSnap = _uiState.value
        val zone = currentZoneId()
        val (events, tasks) = coroutineScope {
            val calColors = stateSnap.availableCalendars
                .filter { !it.backgroundColor.isNullOrBlank() }
                .associate { it.id to it.backgroundColor!! }
            val evDef = async { repo.fetchCalendarEvents(token, stateSnap.selectedCalendarIds, zone, calColors) }
            val tkDef = async {
                repo.fetchTasksForLists(
                    accessToken = token,
                    taskListIds = stateSnap.selectedTaskListIds,
                )
            }
            evDef.await() to tkDef.await()
        }

        val addedCompletionOps = autoDetectPhotoLinks(events, tasks)
        if (addedCompletionOps) {
            flushPendingReminderOps(token)
        }

        // Re-read pending ops right before merge so local edits queued during fetch aren't clobbered.
        val latestOps = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        val mergedReminders = sortReminders(applyUnflushedReminderOps(events + tasks, latestOps))
        val prevReminders = _uiState.value.reminders
        if (mergedReminders != prevReminders) {
            withContext(Dispatchers.IO) { prefs.saveReminders(mergedReminders) }
        }
        Log.d("NernalPerf", "syncGoogleData: synced ${events.size} events and ${tasks.size} tasks (includePhotos=$includePhotos)")
        _uiState.update {
            it.copy(
                reminders = mergedReminders,
                pendingSyncCount = latestOps.size,
                lastGoogleSyncMs = System.currentTimeMillis(),
                googleSyncing = false,
            )
        }
        checkUpcomingEventReminders()

        // 3. Optionally refresh custom shared album links
        if (includePhotos) {
            refreshPhotoAlbumsOnly()
        }
    }

    private suspend fun refreshPhotoAlbumsOnly() {
        val customUrls = _uiState.value.customSharedAlbumUrls

        if (customUrls.isEmpty()) {
            // Keep Picker photos if user picked photos into libraryPool and didn't add shared album links
            val hasOnlyPickerPhotos = libraryPool.isNotEmpty() &&
                libraryPool.all { it.albumUrl == NernalRepository.PICKER_ALBUM_MARKER }
            if (!hasOnlyPickerPhotos) {
                libraryPool = emptyList()
                withContext(Dispatchers.IO) { prefs.clearAlbumLibrary() }
                applyLibraryPoolToState(0, syncDone = true)
            }
            return
        }

        _uiState.update { it.copy(albumsSyncing = true, albumError = null) }

        repo.fetchSharedAlbumPhotos(
            customSharedUrls = customUrls,
            onInitialBatch = { batch ->
                if (_uiState.value.allPhotos.isEmpty()) {
                    libraryPool = batch
                    applyLibraryPoolToState(batch.size, syncDone = false)
                }
            },
        ).onSuccess { catalog ->
            lastPhotosRefreshMs = System.currentTimeMillis()
            // Preserve any Picker photos in pool if present
            val existingPicked = libraryPool.filter { it.albumUrl == NernalRepository.PICKER_ALBUM_MARKER }
            val merged = (catalog + existingPicked).distinctBy { it.id }
            libraryPool = merged
            withContext(Dispatchers.IO) {
                prefs.saveAlbumLibrary(merged, merged.size)
            }
            applyLibraryPoolToState(merged.size, syncDone = true)
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    albumsSyncing = false,
                    albumError = if (it.allPhotos.isEmpty()) err.message ?: "Unable to load selected albums" else null,
                )
            }
        }
    }

    fun toggleCalendarSelection(calendarId: String) {
        val next = _uiState.value.selectedCalendarIds.toMutableSet()
        if (!next.add(calendarId)) next.remove(calendarId)
        prefs.saveSelectedCalendarIds(next)
        _uiState.update { it.copy(selectedCalendarIds = next) }
        viewModelScope.launch { syncGoogleData(includePhotos = false) }
    }

    fun toggleTaskListSelection(listId: String) {
        val next = _uiState.value.selectedTaskListIds.toMutableSet()
        if (!next.add(listId)) next.remove(listId)
        prefs.saveSelectedTaskListIds(next)
        _uiState.update { it.copy(selectedTaskListIds = next) }
        viewModelScope.launch { syncGoogleData(includePhotos = false) }
    }

    fun createGoogleTaskList(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            repo.createTaskList(token, trimmed)
                .onSuccess { created ->
                    val nextAvailable = _uiState.value.availableTaskLists + created
                    val nextSelected = _uiState.value.selectedTaskListIds + created.id
                    prefs.saveSelectedTaskListIds(nextSelected)
                    _uiState.update {
                        it.copy(
                            availableTaskLists = nextAvailable,
                            selectedTaskListIds = nextSelected,
                            syncNotice = "Created Google Task list \"${created.title}\"",
                        )
                    }
                    syncGoogleData(includePhotos = false)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(tasksError = err.message ?: "Unable to create task list") }
                }
        }
    }

    fun addCustomSharedAlbumUrl(rawUrl: String) {
        val cleaned = rawUrl.trim()
        if (!cleaned.startsWith("http")) return
        val next = (_uiState.value.customSharedAlbumUrls + cleaned).distinct()
        prefs.saveCustomSharedAlbumUrls(next)
        _uiState.update { it.copy(customSharedAlbumUrls = next) }
        viewModelScope.launch { refreshPhotoAlbumsOnly() }
    }

    fun removeCustomSharedAlbumUrl(url: String) {
        val next = _uiState.value.customSharedAlbumUrls.filterNot { it == url }
        prefs.saveCustomSharedAlbumUrls(next)
        _uiState.update { it.copy(customSharedAlbumUrls = next) }
        viewModelScope.launch { refreshPhotoAlbumsOnly() }
    }

    private suspend fun autoDetectPhotoLinks(
        events: List<ReminderItem>,
        tasks: List<ReminderItem>,
    ): Boolean {
        val detected = mutableSetOf<String>()
        val tasksToComplete = mutableListOf<ReminderItem>()
        fun scan(text: String?, item: ReminderItem? = null) {
            if (text.isNullOrBlank()) return
            NernalRepository.PHOTO_LINK_REGEX.findAll(text).forEach { match ->
                val clean = match.value.trimEnd('.', ',', ')', '>', ';', '"', '\'')
                if (clean.isNotBlank()) {
                    detected.add(clean)
                    if (item != null && !item.completed && item.category != ReminderCategory.EVENT) {
                        tasksToComplete.add(item)
                    }
                }
            }
        }
        for (item in tasks) {
            scan(item.text, item)
            scan(item.notes, item)
        }
        for (item in events) {
            scan(item.text, null)
            scan(item.notes, null)
        }
        if (detected.isNotEmpty()) {
            val current = _uiState.value.customSharedAlbumUrls.toSet()
            val toAdd = detected - current
            if (toAdd.isNotEmpty()) {
                val next = (current + toAdd).toList()
                withContext(Dispatchers.IO) { prefs.saveCustomSharedAlbumUrls(next) }
                _uiState.update {
                    it.copy(
                        customSharedAlbumUrls = next,
                        syncNotice = "Imported ${toAdd.size} album link(s) and completed task",
                    )
                }
                viewModelScope.launch { refreshPhotoAlbumsOnly() }
            }
        }
        // Auto-complete tasks containing imported photo links in one batch without recursive sync.
        val uniqueToComplete = tasksToComplete.distinctBy { it.id }
            .filter { !it.sourceId.isNullOrBlank() && !it.remoteId.isNullOrBlank() }
        if (uniqueToComplete.isEmpty()) return false

        withContext(Dispatchers.IO) {
            val ops = prefs.loadPendingReminderOps().toMutableList()
            for (task in uniqueToComplete) {
                ops.removeAll { it.action == PendingReminderAction.UPDATE && it.reminderId == task.id }
                ops.add(
                    PendingReminderOp(
                        opId = UUID.randomUUID().toString(),
                        action = PendingReminderAction.UPDATE,
                        reminderId = task.id,
                        category = task.category,
                        completed = true,
                        sourceId = task.sourceId,
                        remoteId = task.remoteId,
                    )
                )
            }
            prefs.savePendingReminderOps(ops)
        }
        return true
    }

    private fun bringAppToFront() {
        runCatching {
            val app = getApplication<Application>()
            val intent = Intent(app, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            app.startActivity(intent)
        }
    }

    fun launchGooglePhotosPicker() {
        viewModelScope.launch {
            val token = authManager.getValidAccessToken() ?: return@launch
            _uiState.update { it.copy(pickerPolling = true, photosApiError = null) }
            repo.createPickerSession(token)
                .onSuccess { session ->
                    _uiState.update { it.copy(pickerSessionUri = session.pickerUri) }
                    // Immediately launch browser so user does not need to tap twice
                    runCatching {
                        val app = getApplication<Application>()
                        val intent = Intent(Intent.ACTION_VIEW, session.pickerUri.toUri()).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                    }.onFailure {
                        _uiState.update {
                            it.copy(syncNotice = "No browser app on frame — open Picker link from another device")
                        }
                    }
                    pickerPollJob?.cancel()
                    pickerPollJob = viewModelScope.launch {
                        repeat(120) {
                            delay(2_500L)
                            val freshToken = authManager.getValidAccessToken() ?: return@launch
                            if (repo.pollPickerSession(freshToken, session.id)) {
                                repo.fetchPickedMediaItems(freshToken, session.id)
                                    .onSuccess { picked ->
                                        lastPhotosRefreshMs = System.currentTimeMillis()
                                        val existing = libraryPool.filterNot {
                                            it.albumUrl == NernalRepository.PICKER_ALBUM_MARKER
                                        }
                                        val combined = (picked + existing).distinctBy { it.id }
                                        libraryPool = combined
                                        withContext(Dispatchers.IO) {
                                            prefs.saveAlbumLibrary(combined, combined.size)
                                        }
                                        applyLibraryPoolToState(
                                            totalCount = combined.size,
                                            syncDone = true,
                                            notice = "Loaded ${picked.size} picked photo(s)",
                                        )
                                        // Warm disk cache while Google Photos Picker baseUrls (~60m TTL) are fresh.
                                        viewModelScope.launch(Dispatchers.IO) {
                                            picked.take(50).forEach { photo ->
                                                repo.loadPhotoBitmap(photo, accessToken = freshToken)
                                            }
                                        }
                                        // Bring Nernal back to front once selection finishes
                                        bringAppToFront()
                                    }
                                    .onFailure { err ->
                                        _uiState.update { it.copy(photosApiError = err.message) }
                                    }
                                _uiState.update { it.copy(pickerSessionUri = null, pickerPolling = false) }
                                return@launch
                            }
                        }
                        _uiState.update { it.copy(pickerPolling = false) }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            pickerPolling = false,
                            photosApiError = err.message ?: "Unable to open Google Photos Picker",
                        )
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Slideshow & Photo Filtering
    // -------------------------------------------------------------------------

    fun refreshOrReshufflePhotos() {
        viewModelScope.launch {
            if (libraryPool.size > ROTATION_WINDOW_SIZE) {
                applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = true)
            } else {
                refreshPhotoAlbumsOnly()
            }
        }
    }

    private fun startSlideshowLoop() {
        viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                val paused = !state.autoAdvance || state.isDimmed || state.showRadarModal ||
                    state.showAccountDialog || state.showSettingsDialog || state.showInAppAuthWebView ||
                    state.activePhotos.size <= 1
                if (paused) {
                    delay(2_500L)
                    continue
                }
                val targetMs = state.slideIntervalSec.coerceIn(5, 120) * 1000L
                val elapsedSinceManual = System.currentTimeMillis() - lastManualNavMs
                val remainingMs = (targetMs - elapsedSinceManual).coerceIn(500L, targetMs)
                delay(remainingMs)

                val latest = _uiState.value
                val stillActive = latest.autoAdvance && !latest.isDimmed && !latest.showRadarModal &&
                    !latest.showAccountDialog && !latest.showSettingsDialog && !latest.showInAppAuthWebView &&
                    latest.activePhotos.size > 1
                if (stillActive && System.currentTimeMillis() - lastManualNavMs >= targetMs) {
                    stepPhoto(delta = 1, manual = false)
                }
            }
        }
    }

    fun nextPhoto() = stepPhoto(delta = 1, manual = true)

    fun previousPhoto() = stepPhoto(delta = -1, manual = true)

    private fun stepPhoto(delta: Int, manual: Boolean) {
        val state = _uiState.value
        val photos = state.activePhotos
        if (photos.isEmpty()) return
        if (manual) {
            tempFullscreenJob?.cancel()
            consecutivePhotoFailures = 0
        }
        val nextIndex = ((state.currentPhotoIndex + delta) % photos.size + photos.size) % photos.size
        lastManualNavMs = System.currentTimeMillis()
        if (manual || state.currentPhotoIndex != nextIndex) {
            _uiState.update { it.copy(currentPhotoIndex = nextIndex) }
            loadCurrentAndPreloadNext()
        }
    }

    private fun loadCurrentAndPreloadNext() {
        photoLoadJob?.cancel()
        val state = _uiState.value
        val photos = state.activePhotos
        val current = state.currentPhoto ?: run {
            _uiState.update { it.copy(currentBitmap = null, photoLoading = false) }
            return
        }

        val cached = repo.getCachedBitmap(current)
        if (cached != null) {
            consecutivePhotoFailures = 0
            _uiState.update { it.copy(currentBitmap = cached, photoLoading = false) }
        } else {
            _uiState.update { it.copy(photoLoading = true) }
        }

        photoLoadJob = viewModelScope.launch {
            val token = if (current.albumUrl == NernalRepository.PICKER_ALBUM_MARKER) {
                authManager.getValidAccessToken()
            } else null
            var bmp = cached ?: repo.loadPhotoBitmap(current, accessToken = token)
            if (bmp == null && isActive) {
                delay(400L)
                if (isActive) {
                    bmp = repo.loadPhotoBitmap(current, accessToken = token)
                }
            }
            if (!isActive) return@launch

            if (_uiState.value.currentPhoto?.id == current.id) {
                if (bmp == null) {
                    consecutivePhotoFailures++
                    if (photos.size > 1 && consecutivePhotoFailures < 3) {
                        stepPhoto(delta = 1, manual = false)
                        return@launch
                    }
                    _uiState.update { it.copy(currentBitmap = null, photoLoading = false) }
                } else {
                    consecutivePhotoFailures = 0
                    _uiState.update { it.copy(currentBitmap = bmp, photoLoading = false) }
                }
            }

            if (photos.size > 1) {
                val idx = _uiState.value.currentPhotoIndex
                val seenIds = hashSetOf(current.id)
                for (offset in listOf(1, -1, 2)) {
                    if (!isActive) break
                    val pIdx = ((idx + offset) % photos.size + photos.size) % photos.size
                    val target = photos[pIdx]
                    if (!seenIds.add(target.id)) continue
                    val targetToken = if (target.albumUrl == NernalRepository.PICKER_ALBUM_MARKER) {
                        token ?: authManager.getValidAccessToken()
                    } else null
                    repo.loadPhotoBitmap(target, accessToken = targetToken)
                }
            }
        }
    }

    fun toggleAutoAdvance() {
        lastManualNavMs = System.currentTimeMillis()
        _uiState.update { it.copy(autoAdvance = !it.autoAdvance) }
    }

    fun toggleFavoritesOnly() {
        lastManualNavMs = System.currentTimeMillis()
        _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly, currentPhotoIndex = 0) }
        applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = !_uiState.value.albumsSyncing)
    }

    fun toggleCurrentStarred() {
        val current = _uiState.value.currentPhoto ?: return
        tempFullscreenJob?.cancel()
        _uiState.update { state ->
            val nextIds = state.starredIds.toMutableSet()
            val added = nextIds.add(current.id)
            if (!added) nextIds.remove(current.id)
            prefs.saveStarredIds(nextIds)

            val nextActive = if (state.favoritesOnly) state.allPhotos.filter { it.id in nextIds } else state.allPhotos
            state.copy(
                starredIds = nextIds,
                activePhotos = nextActive,
                currentPhotoIndex = state.currentPhotoIndex.coerceIn(0, (nextActive.size - 1).coerceAtLeast(0)),
            )
        }
        if (_uiState.value.favoritesOnly) {
            loadCurrentAndPreloadNext()
        }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showAccountDialog = show, syncNotice = if (!show) null else it.syncNotice) }
    }

    fun setSlideIntervalSec(seconds: Int) {
        val clamped = seconds.coerceIn(5, 120)
        prefs.saveSlideIntervalSec(clamped)
        lastManualNavMs = System.currentTimeMillis()
        _uiState.update { it.copy(slideIntervalSec = clamped) }
    }

    fun togglePhotoYear(year: Int) {
        lastManualNavMs = System.currentTimeMillis()
        val available = _uiState.value.availableYears.toSet()
        val currentYears = if (_uiState.value.isAllYearsMode) available else _uiState.value.selectedPhotoYears

        val updated = if (year in currentYears) {
            currentYears - year
        } else {
            currentYears + year
        }
        val isAll = available.isNotEmpty() && updated.size == available.size && updated.containsAll(available)
        if (isAll) {
            prefs.savePhotoYearFilters(null)
            _uiState.update { it.copy(selectedPhotoYears = available, isAllYearsMode = true, currentPhotoIndex = 0) }
        } else if (updated.isEmpty()) {
            prefs.savePhotoYearFilters(emptySet())
            _uiState.update { it.copy(selectedPhotoYears = emptySet(), isAllYearsMode = false, currentPhotoIndex = 0) }
        } else {
            prefs.savePhotoYearFilters(updated)
            _uiState.update { it.copy(selectedPhotoYears = updated, isAllYearsMode = false, currentPhotoIndex = 0) }
        }
        applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = !_uiState.value.albumsSyncing)
    }

    fun toggleAllPhotoYears() {
        lastManualNavMs = System.currentTimeMillis()
        val available = _uiState.value.availableYears.toSet()
        val currentlyAllLit = _uiState.value.isAllYearsSelected || _uiState.value.isAllYearsMode

        if (currentlyAllLit) {
            prefs.savePhotoYearFilters(emptySet())
            _uiState.update { it.copy(selectedPhotoYears = emptySet(), isAllYearsMode = false, currentPhotoIndex = 0) }
        } else {
            prefs.savePhotoYearFilters(null)
            _uiState.update { it.copy(selectedPhotoYears = available, isAllYearsMode = true, currentPhotoIndex = 0) }
        }
        applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = !_uiState.value.albumsSyncing)
    }

    fun toggleActiveTaskListFilter(listId: String) {
        val current = _uiState.value.activeTaskListFilters
        val updated = if (listId in current) current - listId else current + listId
        if (updated.isEmpty()) return
        prefs.saveActiveTaskListFilters(updated)
        _uiState.update { it.copy(activeTaskListFilters = updated) }
    }

    fun setShowHelpDialog(show: Boolean) {
        _uiState.update { it.copy(showHelpDialog = show) }
    }

    fun setShowFeedbackDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showFeedbackDialog = show,
                feedbackNotice = null,
                feedbackError = null,
            )
        }
    }

    fun submitUserFeedback(comment: String, includeLogs: Boolean) {
        val trimmed = comment.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(feedbackError = "Please enter a comment before sending.") }
            return
        }
        val stateSnap = _uiState.value
        _uiState.update { it.copy(isSubmittingFeedback = true, feedbackNotice = null, feedbackError = null) }

        viewModelScope.launch {
            val diagnostics = if (includeLogs) {
                withContext(Dispatchers.IO) { buildDiagnosticLogBundle(stateSnap) }
            } else {
                null
            }
            val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} " +
                "(Android ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT})"
            repo.submitFeedback(
                email = stateSnap.accountProfile?.email,
                displayName = stateSnap.accountProfile?.displayName,
                appId = BuildConfig.APPLICATION_ID,
                appVersion = BuildConfig.VERSION_NAME,
                device = deviceInfo,
                comment = trimmed,
                diagnostics = diagnostics,
            ).onSuccess { id ->
                _uiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        feedbackNotice = "Thank you! Your feedback was sent (#$id).",
                        feedbackError = null,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        feedbackError = err.message ?: "Failed to send feedback",
                    )
                }
            }
        }
    }

    private fun buildDiagnosticLogBundle(state: NernalUiState): String {
        val sb = StringBuilder()
        sb.appendLine("=== App State ===")
        sb.appendLine("authState=${state.authState}")
        sb.appendLine("location=${state.userLocation.name} (${state.userLocation.timezone})")
        sb.appendLine("calendars=${state.selectedCalendarIds.size}/${state.availableCalendars.size}")
        sb.appendLine("taskLists=${state.selectedTaskListIds.size}/${state.availableTaskLists.size}")
        sb.appendLine("reminders=${state.reminders.size}, pendingOps=${state.pendingSyncCount}")
        sb.appendLine("photos=${state.libraryTotalCount} (active=${state.activePhotos.size}, favs=${state.starredIds.size})")
        sb.appendLine("calendarPrimary=${state.calendarPrimaryMode}, viewMode=${state.calendarViewMode}")
        state.authError?.let { sb.appendLine("authError=$it") }
        state.weatherError?.let { sb.appendLine("weatherError=$it") }
        state.calendarError?.let { sb.appendLine("calendarError=$it") }
        state.tasksError?.let { sb.appendLine("tasksError=$it") }
        state.photosApiError?.let { sb.appendLine("photosApiError=$it") }
        state.albumError?.let { sb.appendLine("albumError=$it") }
        sb.appendLine()
        sb.appendLine("=== Recent Process Logcat ===")
        val pid = android.os.Process.myPid()
        val logText = runCatching {
            val proc = ProcessBuilder("logcat", "-d", "-v", "time", "--pid=$pid", "-t", "180")
                .redirectErrorStream(true)
                .start()
            proc.inputStream.bufferedReader().use { it.readText() }
        }.getOrElse { err -> "Unable to read logcat: ${err.message}" }
        sb.append(logText.takeLast(32_000))
        return NernalRepository.scrubSensitiveTokens(sb.toString())
    }

    fun setPhotoAlbumFilter(album: String?) {
        prefs.savePhotoAlbumFilter(album)
        lastManualNavMs = System.currentTimeMillis()
        _uiState.update { it.copy(photoAlbumFilter = album, currentPhotoIndex = 0) }
        applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = !_uiState.value.albumsSyncing)
    }

    fun resetPhotoFilters() {
        prefs.savePhotoYearFilters(null)
        prefs.savePhotoAlbumFilter(null)
        lastManualNavMs = System.currentTimeMillis()
        val allYears = _uiState.value.availableYears.toSet()
        _uiState.update {
            it.copy(
                selectedPhotoYears = allYears,
                isAllYearsMode = true,
                photoAlbumFilter = null,
                favoritesOnly = false,
                currentPhotoIndex = 0,
            )
        }
        applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = !_uiState.value.albumsSyncing)
    }

    fun setDimBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.06f, 0.45f)
        prefs.saveDimBrightness(clamped)
        _uiState.update { it.copy(dimBrightness = clamped) }
    }

    private fun applyLibraryPoolToState(
        totalCount: Int,
        syncDone: Boolean,
        notice: String? = null,
    ) {
        val years = libraryPool.mapNotNull { photo ->
            photo.year?.takeIf { y -> y > 1970 }
                ?: photo.timestampMs?.let { ms ->
                    runCatching { java.time.Instant.ofEpochMilli(ms).atZone(NernalRepository.EASTERN_ZONE).year }.getOrNull()
                }?.takeIf { y -> y > 1970 }
        }.distinct().sortedDescending()
        val albums = libraryPool.map { it.albumTitle }.filter { it.isNotBlank() }.distinct().sorted()

        _uiState.update { s ->
            val saved = prefs.loadPhotoYearFilters()
            val isAll = s.isAllYearsMode || saved == null
            val effectiveYears = when {
                isAll -> if (years.isNotEmpty()) years.toSet() else s.selectedPhotoYears
                saved?.isEmpty() == true -> emptySet()
                s.selectedPhotoYears.isNotEmpty() -> s.selectedPhotoYears.intersect(years.toSet().ifEmpty { s.selectedPhotoYears })
                else -> emptySet()
            }
            val yearFilterSet = if (isAll || effectiveYears.isEmpty()) null else effectiveYears
            val filtered = NernalRepository.filterPhotos(libraryPool, yearFilterSet, s.photoAlbumFilter)
            val baseWindow = NernalRepository.selectBalancedPhotos(
                library = filtered,
                maxCount = ROTATION_WINDOW_SIZE,
                favorites = s.starredIds,
            )
            val finalPool = if (s.favoritesOnly) {
                val extraFavs = filtered.filter { it.id in s.starredIds }
                (baseWindow + extraFavs).distinctBy { it.id }
            } else {
                baseWindow
            }
            val active = if (s.favoritesOnly) {
                finalPool.filter { it.id in s.starredIds }
            } else {
                finalPool
            }
            s.copy(
                allPhotos = finalPool,
                activePhotos = active,
                libraryTotalCount = totalCount,
                availableYears = years,
                selectedPhotoYears = effectiveYears,
                isAllYearsMode = isAll,
                availableAlbums = albums,
                currentPhotoIndex = 0,
                albumsSyncing = !syncDone,
                albumError = null,
                syncNotice = notice ?: s.syncNotice,
            )
        }
        loadCurrentAndPreloadNext()
    }

    private var tempFullscreenJob: kotlinx.coroutines.Job? = null

    fun inspectPhotoTemporary(durationSec: Int = 10) {
        lastManualNavMs = System.currentTimeMillis()
        tempFullscreenJob?.cancel()
        _uiState.update { it.copy(fullscreenMode = true) }
        tempFullscreenJob = viewModelScope.launch {
            delay(durationSec * 1000L)
            _uiState.update { it.copy(fullscreenMode = false) }
            lastManualNavMs = System.currentTimeMillis()
        }
    }

    fun setFullscreen(enabled: Boolean) {
        tempFullscreenJob?.cancel()
        lastManualNavMs = System.currentTimeMillis()
        _uiState.update { it.copy(fullscreenMode = enabled) }
    }

    fun setGlobalBrightness(level: Float) {
        val clamped = level.coerceIn(0.1f, 1.0f)
        prefs.saveGlobalBrightness(clamped)
        _uiState.update { it.copy(globalBrightness = clamped) }
        applySystemBrightness(clamped)
    }

    private fun applySystemBrightness(level: Float) {
        runCatching {
            val app = getApplication<Application>()
            if (android.provider.Settings.System.canWrite(app)) {
                val intVal = (level * 255).toInt().coerceIn(10, 255)
                android.provider.Settings.System.putInt(
                    app.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    intVal,
                )
            }
        }
    }

    fun setDimmed(dimmed: Boolean) {
        val wasDimmed = _uiState.value.isDimmed
        _uiState.update { it.copy(isDimmed = dimmed) }
        if (wasDimmed && !dimmed && _uiState.value.authState == GoogleAuthState.SIGNED_IN) {
            val stale = System.currentTimeMillis() - _uiState.value.lastGoogleSyncMs > GOOGLE_SYNC_INTERVAL_MS
            if (stale) {
                viewModelScope.launch { syncGoogleData(includePhotos = false) }
            }
        }
    }

    fun setWakeOnEventReminder(enabled: Boolean) {
        prefs.saveWakeOnEventReminder(enabled)
        _uiState.update { it.copy(wakeOnEventReminder = enabled) }
        checkUpcomingEventReminders()
    }

    fun dismissEventAlert() {
        _uiState.update { it.copy(activeEventAlert = null) }
    }

    fun setSelectedEventDetail(item: ReminderItem?) {
        _uiState.update { it.copy(selectedEventDetail = item) }
    }

    // -------------------------------------------------------------------------
    // Radar
    // -------------------------------------------------------------------------

    fun openRadarModal() {
        _uiState.update { it.copy(showRadarModal = true) }
        refreshRadar()
    }

    fun closeRadarModal() {
        _uiState.update { it.copy(showRadarModal = false) }
    }

    fun refreshRadar() {
        viewModelScope.launch {
            _uiState.update { it.copy(radarLoading = true) }
            val loc = _uiState.value.userLocation
            repo.fetchRadarData(loc.latitude, loc.longitude)
                .onSuccess { data ->
                    _uiState.update { it.copy(radarData = data, radarLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(radarLoading = false) }
                }
        }
    }

    suspend fun loadRadarTile(url: String, needsAlpha: Boolean): Bitmap? =
        repo.loadMapTile(url, needsAlpha)

    // -------------------------------------------------------------------------
    // Reminders (Calendar Events + Google Tasks)
    // -------------------------------------------------------------------------

    fun setHubFilter(filter: HubFilter) {
        _uiState.update { it.copy(hubFilter = filter) }
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        prefs.saveCalendarViewMode(mode)
        _uiState.update { it.copy(calendarViewMode = mode) }
    }

    fun setCalendarPrimaryMode(enabled: Boolean) {
        Log.d("NernalClick", "setCalendarPrimaryMode called with enabled=$enabled")
        prefs.saveCalendarPrimaryMode(enabled)
        _uiState.update {
            it.copy(
                calendarPrimaryMode = enabled,
                hubFilter = if (enabled) HubFilter.EVENTS else it.hubFilter,
            )
        }
    }

    private val updateManager = AppUpdateManager()

    private fun startUpdateCheckLoop() {
        viewModelScope.launch {
            delay(15_000L)
            while (isActive) {
                checkForUpdates(manual = false)
                delay(6 * 3600_000L)
            }
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            Log.d("NernalUpdate", "checkForUpdates started, manual=$manual")
            _uiState.update { it.copy(isCheckingForUpdate = true, updateError = null, updateNotice = null) }
            val currentVersion = BuildConfig.VERSION_NAME
            when (val result = updateManager.checkForUpdate(currentVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Log.d("NernalUpdate", "Update available: ${result.release.tagName}")
                    _uiState.update {
                        it.copy(
                            isCheckingForUpdate = false,
                            updateRelease = result.release,
                            showUpdateDialog = true,
                            updateNotice = "Update available: ${result.release.tagName}",
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    Log.d("NernalUpdate", "App is up to date (v$currentVersion)")
                    _uiState.update {
                        it.copy(
                            isCheckingForUpdate = false,
                            updateRelease = null,
                            updateNotice = if (manual) "App is up to date (v$currentVersion)" else null,
                        )
                    }
                }
                is UpdateCheckResult.Error -> {
                    Log.w("NernalUpdate", "Update error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isCheckingForUpdate = false,
                            updateError = if (manual) result.message else null,
                        )
                    }
                }
            }
        }
    }

    fun downloadAndInstallUpdate(context: android.content.Context) {
        val release = _uiState.value.updateRelease ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingUpdate = true, updateDownloadProgress = 0f, updateError = null) }
            val downloadResult = updateManager.downloadApk(
                context = context,
                release = release,
                onProgress = { progress ->
                    _uiState.update { it.copy(updateDownloadProgress = progress) }
                }
            )
            downloadResult.fold(
                onSuccess = { apkFile ->
                    _uiState.update { it.copy(isDownloadingUpdate = false) }
                    val installResult = updateManager.promptInstall(context, apkFile)
                    installResult.onFailure { error ->
                        _uiState.update { it.copy(updateError = error.message ?: "Failed to launch installer") }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            updateError = error.message ?: "Download failed",
                        )
                    }
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    private fun resolveTargetSourceId(category: ReminderCategory): String? {
        val state = _uiState.value
        return when (category) {
            ReminderCategory.EVENT -> {
                val writableSelected = state.availableCalendars.filter {
                    it.id in state.selectedCalendarIds && it.isWritable
                }
                writableSelected.firstOrNull { it.primary }?.id
                    ?: writableSelected.firstOrNull()?.id
                    ?: state.selectedCalendarIds.firstOrNull()
                    ?: state.availableCalendars.firstOrNull { it.isWritable }?.id
            }
            ReminderCategory.TASK -> {
                state.selectedTaskListIds.firstOrNull()
                    ?: state.availableTaskLists.firstOrNull()?.id
            }
        }
    }

    fun addReminder(text: String, category: ReminderCategory) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        val today = _uiState.value.todayKey
        val targetSourceId = resolveTargetSourceId(category)
        val tempId = "local-${UUID.randomUUID()}"
        val newItem = ReminderItem(
            id = tempId,
            text = cleaned,
            category = category,
            dateKey = if (category == ReminderCategory.EVENT) today else null,
            completed = false,
            createdAtMs = System.currentTimeMillis(),
            sourceId = targetSourceId,
        )
        var snapshotReminders: List<ReminderItem> = emptyList()
        _uiState.update { state ->
            val updated = sortReminders(listOf(newItem) + state.reminders)
            snapshotReminders = updated
            state.copy(reminders = updated)
        }
        checkUpcomingEventReminders()

        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN && targetSourceId != null) {
                val ops = withContext(Dispatchers.IO) {
                    val list = prefs.loadPendingReminderOps().toMutableList()
                    list.add(
                        PendingReminderOp(
                            opId = UUID.randomUUID().toString(),
                            action = PendingReminderAction.ADD,
                            reminderId = tempId,
                            text = newItem.text,
                            dateKey = newItem.dateKey,
                            category = category,
                            completed = false,
                            sourceId = targetSourceId,
                        )
                    )
                    prefs.savePendingReminderOps(list)
                    list
                }
                _uiState.update { it.copy(pendingSyncCount = ops.size) }
                syncGoogleData(includePhotos = false)
            }
        }
    }

    fun toggleReminder(id: String) {
        var newCompleted = false
        var targetItem: ReminderItem? = null
        var snapshotReminders: List<ReminderItem> = emptyList()
        _uiState.update { state ->
            val updated = sortReminders(
                state.reminders.map {
                    if (it.id == id) {
                        newCompleted = !it.completed
                        val toggled = it.copy(completed = newCompleted)
                        targetItem = toggled
                        toggled
                    } else it
                }
            )
            snapshotReminders = updated
            state.copy(reminders = updated)
        }
        checkUpcomingEventReminders()

        val item = targetItem ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
            // Google Tasks supports completion status; Google Calendar events toggle locally only.
            if (item.category != ReminderCategory.EVENT && _uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                val ops = withContext(Dispatchers.IO) {
                    val list = prefs.loadPendingReminderOps().toMutableList()
                    val pendingAddIdx = list.indexOfFirst { it.action == PendingReminderAction.ADD && it.reminderId == id }
                    if (pendingAddIdx >= 0) {
                        list[pendingAddIdx] = list[pendingAddIdx].copy(completed = newCompleted)
                    } else if (!item.sourceId.isNullOrBlank() && !item.remoteId.isNullOrBlank()) {
                        list.removeAll { it.action == PendingReminderAction.UPDATE && it.reminderId == id }
                        list.add(
                            PendingReminderOp(
                                opId = UUID.randomUUID().toString(),
                                action = PendingReminderAction.UPDATE,
                                reminderId = id,
                                category = item.category,
                                completed = newCompleted,
                                sourceId = item.sourceId,
                                remoteId = item.remoteId,
                            )
                        )
                    }
                    prefs.savePendingReminderOps(list)
                    list
                }
                _uiState.update { it.copy(pendingSyncCount = ops.size) }
                syncGoogleData(includePhotos = false)
            }
        }
    }

    fun deleteReminder(id: String) {
        val existing = _uiState.value.reminders.firstOrNull { it.id == id } ?: return
        val priorPending = pendingUndoDeleteItem
        undoDeleteJob?.cancel()
        pendingUndoDeleteItem = existing

        var snapshotReminders: List<ReminderItem> = emptyList()
        _uiState.update { state ->
            val updated = state.reminders.filterNot { it.id == id }
            snapshotReminders = updated
            state.copy(reminders = updated, recentlyDeletedReminder = existing)
        }
        checkUpcomingEventReminders()

        undoDeleteJob = viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
            if (priorPending != null) {
                commitDeleteReminderOp(priorPending)
            }
            delay(5_000L)
            pendingUndoDeleteItem = null
            _uiState.update { it.copy(recentlyDeletedReminder = null) }
            commitDeleteReminderOp(existing)
        }
    }

    fun undoDeleteReminder() {
        val toRestore = pendingUndoDeleteItem ?: _uiState.value.recentlyDeletedReminder ?: return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        pendingUndoDeleteItem = null

        var snapshotReminders: List<ReminderItem> = emptyList()
        _uiState.update { state ->
            val updated = sortReminders((state.reminders + toRestore).distinctBy { it.id })
            snapshotReminders = updated
            state.copy(reminders = updated, recentlyDeletedReminder = null)
        }
        checkUpcomingEventReminders()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
        }
    }

    private suspend fun commitDeleteReminderOp(existing: ReminderItem) {
        if (_uiState.value.authState != GoogleAuthState.SIGNED_IN) return
        val ops = withContext(Dispatchers.IO) {
            val list = prefs.loadPendingReminderOps().toMutableList()
            val wasUnsyncedLocal = list.any { it.action == PendingReminderAction.ADD && it.reminderId == existing.id }
            list.removeAll { it.reminderId == existing.id }
            if (!wasUnsyncedLocal && !existing.sourceId.isNullOrBlank() && !existing.remoteId.isNullOrBlank()) {
                list.add(
                    PendingReminderOp(
                        opId = UUID.randomUUID().toString(),
                        action = PendingReminderAction.DELETE,
                        reminderId = existing.id,
                        category = existing.category,
                        sourceId = existing.sourceId,
                        remoteId = existing.remoteId,
                    )
                )
            }
            prefs.savePendingReminderOps(list)
            list
        }
        _uiState.update { it.copy(pendingSyncCount = ops.size) }
        syncGoogleData(includePhotos = false)
    }

    private suspend fun flushPendingReminderOps(accessToken: String): List<PendingReminderOp> {
        val pending = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        if (pending.isEmpty()) return emptyList()

        val remaining = mutableListOf<PendingReminderOp>()
        val tempRemoteMap = mutableMapOf<String, Pair<String, String>>()
        val zone = currentZoneId()
        var halted = false
        var skippedNotice: String? = null

        for (op in pending) {
            if (halted) {
                remaining.add(op)
                continue
            }
            val mapped = tempRemoteMap[op.reminderId]
            val srcId = mapped?.first ?: op.sourceId
            val remId = mapped?.second ?: op.remoteId

            when (op.action) {
                PendingReminderAction.ADD -> {
                    val targetSource = srcId ?: resolveTargetSourceId(op.category)
                    if (targetSource.isNullOrBlank()) {
                        remaining.add(op)
                        continue
                    }
                    when (
                        val res = if (op.category == ReminderCategory.EVENT) {
                            repo.createCalendarEvent(accessToken, targetSource, op.text, op.dateKey, zone)
                        } else {
                            repo.createTask(accessToken, targetSource, op.text, op.category)
                        }
                    ) {
                        is RemoteMutationResult.Success -> {
                            val remoteId = res.value.remoteId
                            if (remoteId != null) {
                                tempRemoteMap[op.reminderId] = targetSource to remoteId
                                if (op.completed && op.category != ReminderCategory.EVENT) {
                                    when (val patchRes = repo.updateTaskCompletion(accessToken, targetSource, remoteId, true)) {
                                        is RemoteMutationResult.TransientError -> {
                                            halted = true
                                            remaining.add(
                                                PendingReminderOp(
                                                    opId = UUID.randomUUID().toString(),
                                                    action = PendingReminderAction.UPDATE,
                                                    reminderId = op.reminderId,
                                                    category = op.category,
                                                    completed = true,
                                                    sourceId = targetSource,
                                                    remoteId = remoteId,
                                                )
                                            )
                                        }
                                        is RemoteMutationResult.PermanentError -> {
                                            Log.w("NernalSync", "Dropped permanent failure on ADD completion: ${patchRes.message}")
                                        }
                                        is RemoteMutationResult.Success -> Unit
                                    }
                                }
                            }
                        }
                        is RemoteMutationResult.TransientError -> {
                            halted = true
                            remaining.add(op)
                        }
                        is RemoteMutationResult.PermanentError -> {
                            Log.w("NernalSync", "Dropped poison ADD op (${op.text}): HTTP ${res.httpCode} ${res.message}")
                            skippedNotice = "Skipped rejected sync item: ${res.message}"
                        }
                    }
                }
                PendingReminderAction.UPDATE -> {
                    if (op.category == ReminderCategory.EVENT || srcId.isNullOrBlank() || remId.isNullOrBlank()) {
                        continue
                    }
                    when (val res = repo.updateTaskCompletion(accessToken, srcId, remId, op.completed)) {
                        is RemoteMutationResult.Success -> Unit
                        is RemoteMutationResult.TransientError -> {
                            halted = true
                            remaining.add(op.copy(sourceId = srcId, remoteId = remId))
                        }
                        is RemoteMutationResult.PermanentError -> {
                            Log.w("NernalSync", "Dropped poison UPDATE op (${op.reminderId}): HTTP ${res.httpCode} ${res.message}")
                            skippedNotice = "Skipped rejected sync item: ${res.message}"
                        }
                    }
                }
                PendingReminderAction.DELETE -> {
                    if (srcId.isNullOrBlank() || remId.isNullOrBlank()) continue
                    val res = if (op.category == ReminderCategory.EVENT) {
                        repo.deleteCalendarEvent(accessToken, srcId, remId)
                    } else {
                        repo.deleteTask(accessToken, srcId, remId)
                    }
                    when (res) {
                        is RemoteMutationResult.Success -> Unit
                        is RemoteMutationResult.TransientError -> {
                            halted = true
                            remaining.add(op.copy(sourceId = srcId, remoteId = remId))
                        }
                        is RemoteMutationResult.PermanentError -> {
                            Log.w("NernalSync", "Dropped poison DELETE op (${op.reminderId}): HTTP ${res.httpCode} ${res.message}")
                            skippedNotice = "Skipped rejected sync item: ${res.message}"
                        }
                    }
                }
            }
        }

        if (remaining != pending) {
            withContext(Dispatchers.IO) { prefs.savePendingReminderOps(remaining) }
        }
        if (skippedNotice != null) {
            _uiState.update { it.copy(syncNotice = skippedNotice) }
        }
        return remaining
    }

    private fun applyUnflushedReminderOps(
        base: List<ReminderItem>,
        ops: List<PendingReminderOp>,
    ): List<ReminderItem> {
        val map = base.associateBy { it.id }.toMutableMap()
        for (op in ops) {
            when (op.action) {
                PendingReminderAction.ADD -> map[op.reminderId] = ReminderItem(
                    id = op.reminderId,
                    text = op.text,
                    category = op.category,
                    dateKey = op.dateKey,
                    time = op.time,
                    completed = op.completed,
                    sourceId = op.sourceId,
                    remoteId = op.remoteId,
                )
                PendingReminderAction.UPDATE -> map[op.reminderId]?.let {
                    map[op.reminderId] = it.copy(completed = op.completed)
                }
                PendingReminderAction.DELETE -> map.remove(op.reminderId)
            }
        }
        return map.values.toList()
    }

    private companion object {
        const val WEATHER_REFRESH_MS = 15 * 60_000L
        const val GOOGLE_SYNC_INTERVAL_MS = 60_000L
        const val GOOGLE_SYNC_DIMMED_INTERVAL_MS = 5 * 60_000L
        const val PHOTOS_URL_REFRESH_MS = 45 * 60_000L
        const val ROTATION_WINDOW_SIZE = 400

        val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
        val AMPM_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("a", Locale.US)
        val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)
        val DATE_KEY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    }
}
