package com.nernal.frame

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nernal.frame.data.AlarmBriefTiming
import com.nernal.frame.data.AlarmConfig
import com.nernal.frame.data.AlbumPhoto
import com.nernal.frame.data.AppReleaseInfo
import com.nernal.frame.data.AppUpdateManager
import com.nernal.frame.data.CalendarViewMode
import com.nernal.frame.data.ConversationTurn
import com.nernal.frame.data.DeviceCapabilities
import com.nernal.frame.data.FramePrefs
import com.nernal.frame.data.GeminiAssistantClient
import com.nernal.frame.data.GeminiKeyCandidate
import com.nernal.frame.data.GoogleAccountProfile
import com.nernal.frame.data.GoogleAuthManager
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.GoogleCalendarInfo
import com.nernal.frame.data.GoogleTaskListInfo
import com.nernal.frame.data.MemoryEntry
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.OnDeviceVoiceEngine
import com.nernal.frame.data.PendingFavOp
import com.nernal.frame.data.PendingReminderAction
import com.nernal.frame.data.PendingReminderOp
import com.nernal.frame.data.PiConnectionState
import com.nernal.frame.data.QuickAnswerCardData
import com.nernal.frame.data.RadarData
import com.nernal.frame.data.ReminderCategory
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.RemoteMutationResult
import com.nernal.frame.data.TokenResult
import com.nernal.frame.data.UpdateCheckResult
import com.nernal.frame.data.UserLocation
import com.nernal.frame.data.VoiceAction
import com.nernal.frame.data.VoiceAssistantState
import com.nernal.frame.data.VoiceContextSnapshot
import com.nernal.frame.data.WeatherAlert
import com.nernal.frame.data.WeatherData
import com.nernal.frame.data.isEventWithinReminderWindow
import com.nernal.frame.data.shouldAutoCloseEventAlert
import com.nernal.frame.data.sortReminders
import com.nernal.frame.hardware.CameraMotionController
import com.nernal.frame.hardware.DeviceCapabilitiesDetector
import com.nernal.frame.hardware.VoiceAndAudioController
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
    val clockDayDate: String = "",
    val todayKey: String = "",
    val userLocation: UserLocation = UserLocation.DEFAULT,
    val locationSearchResults: List<UserLocation> = emptyList(),
    val locationSearching: Boolean = false,
    val weather: WeatherData? = null,
    val weatherLoading: Boolean = true,
    val weatherError: String? = null,
    val weatherCollapsed: Boolean = false,
    val dismissedAlertKeys: Set<String> = emptySet(),
    val reminders: List<ReminderItem> = emptyList(),
    val showTasks: Boolean = true,
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
    val isManualNightMode: Boolean = false,
    val alarmConfig: AlarmConfig = AlarmConfig(),
    val activeAlarmRinging: Boolean = false,
    val alarmSnoozeUntilMs: Long? = null,
    val showRadarModal: Boolean = false,
    val radarData: RadarData? = null,
    val radarLoading: Boolean = false,
    // Optional Raspberry Pi Two-Way Sync
    val piSyncEnabled: Boolean = BuildConfig.DEFAULT_PI_SYNC_ENABLED,
    val showPiSyncDialog: Boolean = false,
    val piBaseUrl: String = FramePrefs.DEFAULT_PI_URL,
    val activePiUrl: String? = null,
    val piConnection: PiConnectionState = PiConnectionState.STANDALONE,
    val lastPiSyncMs: Long = 0L,
    // Hardware Capabilities, Camera Motion Wake, Audio & Gemini Voice
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val cameraMotionWakeEnabled: Boolean = true,
    val motionDetected: Boolean = false,
    val cameraError: String? = null,
    val autoSleepMinutes: Int = FramePrefs.DEFAULT_AUTO_SLEEP_MINUTES,
    val audibleEventAlertsEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val geminiKeyLanUrl: String? = null,
    val geminiKeyCandidates: List<GeminiKeyCandidate> = emptyList(),
    val geminiKeyScanNotice: String? = null,
    val voiceState: VoiceAssistantState = VoiceAssistantState.IDLE,
    val voiceUserTranscript: String = "",
    val voiceAssistantTranscript: String = "",
    val voiceError: String? = null,
    val isFollowUpListening: Boolean = false,
    val quickAnswerCard: QuickAnswerCardData? = null,
    val memoryEntries: List<MemoryEntry> = emptyList(),
    val showMemoryDialog: Boolean = false,
    val needsAudioPermissionPrompt: Boolean = false,
    val needsCameraPermissionPrompt: Boolean = false,
    val showOverlayPermissionPrompt: Boolean = false,
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
    val defaultTaskListId: String? = null,
    val tasksError: String? = null,
    val selectedWeatherDayIso: String? = null,
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
    val lastAddCategory: ReminderCategory = ReminderCategory.TASK,
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

    val activeWeatherAlerts: List<WeatherAlert>
        get() = weather?.alerts.orEmpty().filterNot { it.alertKey() in dismissedAlertKeys }

    val showFirstSetupShortcuts: Boolean
        get() = authState != GoogleAuthState.SIGNED_IN && customSharedAlbumUrls.isEmpty() && !piSyncEnabled

    val canSpeakAloud: Boolean
        get() = capabilities.hasAudioOutput && (capabilities.hasTextToSpeech || geminiApiKey.isNotBlank())
}

class NernalViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = FramePrefs(application)
    private val repo = NernalRepository(application.cacheDir)
    private val authManager = GoogleAuthManager(prefs, repo.httpClient)
    private val geminiClient = GeminiAssistantClient(repo.httpClient)

    private val _uiState = MutableStateFlow(NernalUiState())
    val uiState: StateFlow<NernalUiState> = _uiState.asStateFlow()

    private val syncMutex = Mutex()
    private val piSyncMutex = Mutex()
    private var libraryPool: List<AlbumPhoto> = emptyList()
    private var lastManualNavMs: Long = 0L
    private var lastPhotosRefreshMs: Long = 0L
    private var lastUserActivityMs: Long = System.currentTimeMillis()
    private var pickerPollJob: Job? = null
    private var photoLoadJob: Job? = null
    private var undoDeleteJob: Job? = null
    private var voiceJob: Job? = null
    private var speakJob: Job? = null
    private var voiceErrorClearJob: Job? = null
    private var alarmChimeJob: Job? = null
    private var alarmSnoozeJob: Job? = null
    private var lastFiredAlarmKey: String? = null
    private var pendingUndoDeleteItem: ReminderItem? = null
    private var consecutivePhotoFailures = 0

    private var cameraController: CameraMotionController? = null
    private var voiceController: VoiceAndAudioController? = null
    private val conversationHistory = mutableListOf<ConversationTurn>()
    private var voiceSessionCancelled = false

    init {
        val detectedCapabilities = DeviceCapabilitiesDetector.detect(application)
        val savedLocation = prefs.loadUserLocation()
        val tokens = prefs.loadGoogleTokens()
        val profile = prefs.loadAccountProfile()
        val remOps = prefs.loadPendingReminderOps()
        val favOps = prefs.loadPendingFavOps()
        val piSyncEnabled = prefs.loadPiSyncEnabled()
        val initialAuthState = if (tokens != null) GoogleAuthState.SIGNED_IN else GoogleAuthState.NOT_SIGNED_IN
        val savedYears = prefs.loadPhotoYearFilters()
        val isAllYears = savedYears == null
        val selTaskLists = prefs.loadSelectedTaskListIds()
        val savedActiveTaskListFilters = prefs.loadActiveTaskListFilters() ?: selTaskLists
        val cameraWakePref = prefs.loadCameraMotionWakeEnabled()
        val autoSleepPref = prefs.loadAutoSleepMinutes()
        val audibleAlertsPref = prefs.loadAudibleEventAlertsEnabled()
        val savedGeminiKey = prefs.loadGeminiApiKey()
        val localMemories = prefs.loadLocalMemoryEntries()

        _uiState.update {
            it.copy(
                userLocation = savedLocation ?: UserLocation.DEFAULT,
                weatherCollapsed = prefs.loadWeatherCollapsed(),
                dismissedAlertKeys = prefs.loadDismissedAlertKeys(),
                alarmConfig = prefs.loadAlarmConfig(),
                reminders = sortReminders(prefs.loadReminders()),
                starredIds = prefs.loadStarredIds(),
                piSyncEnabled = piSyncEnabled,
                piBaseUrl = prefs.loadPiBaseUrl(),
                piConnection = if (piSyncEnabled) PiConnectionState.CHECKING else PiConnectionState.STANDALONE,
                capabilities = detectedCapabilities,
                cameraMotionWakeEnabled = cameraWakePref,
                autoSleepMinutes = autoSleepPref,
                audibleEventAlertsEnabled = audibleAlertsPref,
                geminiApiKey = savedGeminiKey,
                memoryEntries = localMemories,
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
                defaultTaskListId = prefs.loadDefaultTaskListId(),
                activeTaskListFilters = savedActiveTaskListFilters,
                customSharedAlbumUrls = prefs.loadCustomSharedAlbumUrls(),
                pendingSyncCount = remOps.size + if (piSyncEnabled) favOps.size else 0,
                wakeOnEventReminder = prefs.loadWakeOnEventReminder(),
                globalBrightness = prefs.loadGlobalBrightness(),
                calendarViewMode = prefs.loadCalendarViewMode(),
                calendarPrimaryMode = prefs.loadCalendarPrimaryMode(),
                lastAddCategory = prefs.loadLastAddCategory(),
                showOverlayPermissionPrompt = !Settings.canDrawOverlays(application) && !prefs.hasPromptedOverlayPermission(),
                showTasks = prefs.loadShowTasks(),
            )
        }

        setupHardwareControllers(application, detectedCapabilities)
        updateClock()

        startClockLoop()
        startWeatherLoop()
        startInitialLoadAndSyncLoops()
        startSlideshowLoop()
        startIdleSleepLoop()
        if (!BuildConfig.IS_NERNAL_FAMILY_BUILD) {
            startUpdateCheckLoop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        authManager.stopAuthServer()
        authManager.stopGeminiKeyServer()
        pickerPollJob?.cancel()
        photoLoadJob?.cancel()
        undoDeleteJob?.cancel()
        voiceJob?.cancel()
        alarmChimeJob?.cancel()
        alarmSnoozeJob?.cancel()
        cameraController?.release()
        voiceController?.release()
    }

    private fun setupHardwareControllers(
        application: Application,
        capabilities: DeviceCapabilities,
    ) {
        if (capabilities.hasCamera) {
            cameraController = CameraMotionController(
                context = application,
                cameraId = capabilities.preferredCameraId,
                onMotionSample = { detected ->
                    if (detected) {
                        lastUserActivityMs = System.currentTimeMillis()
                    }
                    _uiState.update { s ->
                        if (s.motionDetected == detected) s else s.copy(motionDetected = detected)
                    }
                    if (detected && _uiState.value.isDimmed && !_uiState.value.isManualNightMode && _uiState.value.cameraMotionWakeEnabled) {
                        viewModelScope.launch(Dispatchers.Main) { setDimmed(false) }
                    }
                },
                onStatusError = { err ->
                    _uiState.update { it.copy(cameraError = err) }
                },
            ).also { ctrl ->
                if (_uiState.value.cameraMotionWakeEnabled && ctrl.hasPermission()) {
                    ctrl.setMotionWakeEnabled(true)
                }
            }
        }

        if (capabilities.hasAudioOutput || capabilities.hasMicrophone) {
            voiceController = VoiceAndAudioController(
                context = application,
                onPartialTranscript = { partial ->
                    _uiState.update { it.copy(voiceUserTranscript = partial, voiceError = null) }
                },
                onFinalTranscript = { finalText ->
                    handleFinalVoiceTranscript(finalText)
                },
                onSpeechError = { errMsg ->
                    val wasFollowUp = _uiState.value.isFollowUpListening
                    if (wasFollowUp && (errMsg == "Didn't catch that." || errMsg == "No speech detected.")) {
                        closeVoiceSession()
                    } else {
                        showTransientVoiceError(errMsg)
                    }
                },
                onSpeakingStarted = {
                    _uiState.update { it.copy(voiceState = VoiceAssistantState.SPEAKING) }
                },
                onSpeakingFinished = {
                    if (_uiState.value.voiceState == VoiceAssistantState.SPEAKING) {
                        _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
                    }
                },
            )
        }
    }

    fun touchUserActivity() {
        lastUserActivityMs = System.currentTimeMillis()
    }

    fun onForegroundChanged(active: Boolean) {
        cameraController?.setForegroundActive(active)
        if (active) touchUserActivity()
    }

    fun onPermissionsUpdated() {
        val ctrl = cameraController
        if (ctrl != null && _uiState.value.cameraMotionWakeEnabled && ctrl.hasPermission()) {
            ctrl.setMotionWakeEnabled(true)
            _uiState.update { it.copy(cameraError = null, needsCameraPermissionPrompt = false) }
        }
        if (voiceController?.hasRecordAudioPermission() == true) {
            val pendingAudioPrompt = _uiState.value.needsAudioPermissionPrompt
            _uiState.update { it.copy(needsAudioPermissionPrompt = false) }
            if (pendingAudioPrompt) {
                startVoiceListening(isFollowUp = false)
            }
        }
    }

    fun clearPermissionPrompts() {
        _uiState.update { it.copy(needsAudioPermissionPrompt = false, needsCameraPermissionPrompt = false) }
    }

    fun dismissOverlayPermissionPrompt() {
        prefs.setPromptedOverlayPermission(true)
        _uiState.update { it.copy(showOverlayPermissionPrompt = false) }
    }

    fun setCameraMotionWakeEnabled(enabled: Boolean) {
        prefs.saveCameraMotionWakeEnabled(enabled)
        _uiState.update { it.copy(cameraMotionWakeEnabled = enabled) }
        val ctrl = cameraController ?: return
        if (enabled && !ctrl.hasPermission()) {
            _uiState.update { it.copy(needsCameraPermissionPrompt = true) }
            return
        }
        ctrl.setMotionWakeEnabled(enabled)
    }

    fun setAutoSleepMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 60)
        prefs.saveAutoSleepMinutes(clamped)
        touchUserActivity()
        _uiState.update { it.copy(autoSleepMinutes = clamped) }
    }

    fun setAudibleEventAlertsEnabled(enabled: Boolean) {
        prefs.saveAudibleEventAlertsEnabled(enabled)
        _uiState.update { it.copy(audibleEventAlertsEnabled = enabled) }
    }

    fun saveGeminiApiKey(apiKey: String) {
        val clean = apiKey.trim()
        prefs.saveGeminiApiKey(clean)
        authManager.stopGeminiKeyServer()
        _uiState.update {
            it.copy(
                geminiApiKey = clean,
                geminiKeyLanUrl = null,
                geminiKeyCandidates = emptyList(),
                geminiKeyScanNotice = if (clean.isNotEmpty()) {
                    "Gemini API key saved — Super ${BuildConfig.APP_DISPLAY_NAME} unlocked"
                } else {
                    "Gemini API key cleared — using on-device speech"
                },
                syncNotice = if (clean.isNotEmpty()) {
                    "Gemini API key saved — Super ${BuildConfig.APP_DISPLAY_NAME} unlocked"
                } else {
                    "Gemini API key cleared — using on-device speech"
                },
            )
        }
    }

    fun startGeminiKeyLanReceiver() {
        authManager.startGeminiKeyServer(viewModelScope) { receivedKey ->
            viewModelScope.launch(Dispatchers.Main) {
                saveGeminiApiKey(receivedKey)
            }
        }.onSuccess { url ->
            _uiState.update {
                it.copy(
                    geminiKeyLanUrl = url,
                    geminiKeyScanNotice = "Open $url on your phone or laptop on the same Wi-Fi to paste your key.",
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    geminiKeyLanUrl = null,
                    geminiKeyScanNotice = err.message ?: "Unable to start local Wi-Fi receiver",
                )
            }
        }
    }

    fun stopGeminiKeyLanReceiver() {
        authManager.stopGeminiKeyServer()
        _uiState.update { it.copy(geminiKeyLanUrl = null) }
    }

    fun scanTasksForGeminiKeys() {
        viewModelScope.launch {
            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                syncGoogleData(includePhotos = false)
            }
            val candidates = NernalRepository.extractGeminiKeyCandidates(_uiState.value.reminders)
            _uiState.update {
                it.copy(
                    geminiKeyCandidates = candidates,
                    geminiKeyScanNotice = if (candidates.isEmpty()) {
                        "No Gemini API key (AIza...) found in your Google Tasks. Add one as a task title or note, then tap Scan again."
                    } else {
                        "Found ${candidates.size} key candidate${if (candidates.size == 1) "" else "s"} — tap Use Key below to import."
                    },
                )
            }
        }
    }

    fun importGeminiKeyCandidate(candidate: GeminiKeyCandidate, deleteTaskAfterImport: Boolean = true) {
        saveGeminiApiKey(candidate.apiKey)
        if (deleteTaskAfterImport) {
            deleteReminder(candidate.taskId)
        }
    }

    fun testAudioChimeAndVoice() {
        val vc = voiceController ?: return
        vc.playChime()
        if (_uiState.value.canSpeakAloud) {
            speakWithBestEngine(
                text = "${BuildConfig.APP_DISPLAY_NAME} audio alerts are ready.",
                showOverlay = false,
            )
        }
    }

    fun speakAloud(text: String) {
        if (!_uiState.value.capabilities.hasAudioOutput) return
        touchUserActivity()
        speakWithBestEngine(text = text, showOverlay = true)
    }

    private fun showTransientVoiceError(message: String, durationMs: Long = 5_000L) {
        voiceErrorClearJob?.cancel()
        _uiState.update {
            it.copy(
                voiceState = VoiceAssistantState.IDLE,
                isFollowUpListening = false,
                voiceError = message,
            )
        }
        voiceErrorClearJob = viewModelScope.launch {
            delay(durationMs)
            _uiState.update { cur ->
                if (cur.voiceState == VoiceAssistantState.IDLE && cur.voiceError == message) {
                    cur.copy(voiceError = null, voiceAssistantTranscript = "", voiceUserTranscript = "")
                } else {
                    cur
                }
            }
        }
    }

    private fun speakWithBestEngine(
        text: String,
        showOverlay: Boolean,
        onDone: () -> Unit = {},
    ) {
        val vc = voiceController ?: run {
            onDone()
            return
        }
        val state = _uiState.value
        if (!state.capabilities.hasAudioOutput) {
            onDone()
            return
        }

        speakJob?.cancel()
        voiceErrorClearJob?.cancel()
        val apiKey = state.geminiApiKey.trim()

        if (apiKey.isNotBlank()) {
            speakJob = viewModelScope.launch {
                if (showOverlay) {
                    _uiState.update {
                        it.copy(
                            voiceState = VoiceAssistantState.SPEAKING,
                            voiceAssistantTranscript = text,
                            voiceError = null,
                        )
                    }
                }
                val ttsRes = geminiClient.synthesizeSpeech(apiKey = apiKey, text = text)
                if (!isActive) return@launch
                ttsRes.fold(
                    onSuccess = { wavBytes ->
                        vc.playWavBytes(wavBytes) {
                            _uiState.update { cur ->
                                if (cur.voiceState == VoiceAssistantState.SPEAKING) {
                                    cur.copy(voiceState = VoiceAssistantState.IDLE)
                                } else {
                                    cur
                                }
                            }
                            onDone()
                        }
                    },
                    onFailure = { err ->
                        if (state.capabilities.hasTextToSpeech) {
                            vc.speak(text) {
                                _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
                                onDone()
                            }
                        } else {
                            if (showOverlay) {
                                showTransientVoiceError(err.message ?: "Gemini voice unavailable.")
                            } else {
                                _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
                            }
                            onDone()
                        }
                    },
                )
            }
        } else if (state.capabilities.hasTextToSpeech) {
            if (showOverlay) {
                _uiState.update {
                    it.copy(
                        voiceState = VoiceAssistantState.SPEAKING,
                        voiceAssistantTranscript = text,
                        voiceError = null,
                    )
                }
            }
            vc.speak(text) {
                _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
                onDone()
            }
        } else {
            if (showOverlay) {
                showTransientVoiceError("No built-in voice engine on this frame. Add a Gemini API key in Settings to enable Read Aloud.")
            } else {
                _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
            }
            onDone()
        }
    }

    fun onSpeakButtonClicked() {
        touchUserActivity()
        val state = _uiState.value
        if (!state.capabilities.hasVoiceInputAndOutput) return
        if (state.voiceState != VoiceAssistantState.IDLE) {
            closeVoiceSession()
            return
        }
        val vc = voiceController ?: return
        if (!vc.hasRecordAudioPermission()) {
            _uiState.update { it.copy(needsAudioPermissionPrompt = true) }
            return
        }
        conversationHistory.clear()
        startVoiceListening(isFollowUp = false)
    }

    private fun startVoiceListening(isFollowUp: Boolean) {
        val vc = voiceController ?: return
        voiceSessionCancelled = false
        _uiState.update {
            it.copy(
                voiceState = VoiceAssistantState.LISTENING,
                isFollowUpListening = isFollowUp,
                voiceUserTranscript = "",
                voiceAssistantTranscript = if (isFollowUp) it.voiceAssistantTranscript else "",
                voiceError = null,
            )
        }
        vc.startListening()
    }

    fun closeVoiceSession() {
        voiceSessionCancelled = true
        voiceJob?.cancel()
        speakJob?.cancel()
        voiceErrorClearJob?.cancel()
        voiceController?.stopListening()
        voiceController?.stopSpeaking()
        conversationHistory.clear()
        _uiState.update {
            it.copy(
                voiceState = VoiceAssistantState.IDLE,
                isFollowUpListening = false,
                voiceUserTranscript = "",
                voiceAssistantTranscript = "",
                voiceError = null,
            )
        }
    }

    fun dismissQuickAnswerCard() {
        _uiState.update { it.copy(quickAnswerCard = null) }
    }

    fun setShowMemoryDialog(show: Boolean) {
        touchUserActivity()
        _uiState.update { it.copy(showMemoryDialog = show) }
    }

    fun deleteLocalMemoryEntry(id: String) {
        val remaining = prefs.loadLocalMemoryEntries().filterNot { it.id == id }
        prefs.saveLocalMemoryEntries(remaining)
        val merged = mergeMemoryEntries(remaining, _uiState.value.memoryEntries.filter { it.fromPi })
        _uiState.update { it.copy(memoryEntries = merged) }
    }

    private fun currentVoiceContextSnapshot(): VoiceContextSnapshot {
        val state = _uiState.value
        return VoiceContextSnapshot(
            clockTime = state.clockTime,
            clockAmPm = state.clockAmPm,
            clockDate = state.clockDate,
            todayKey = state.todayKey,
            locationName = state.userLocation.name,
            weather = state.weather,
            reminders = state.reminders,
            isFullscreen = state.fullscreenMode,
            isCalendarPrimary = state.calendarPrimaryMode,
            showRadarModal = state.showRadarModal,
        )
    }

    fun updateAlarmConfig(config: AlarmConfig) {
        val normalized = config.copy(
            hour24 = config.hour24.coerceIn(0, 23),
            minute = config.minute.coerceIn(0, 59),
            daysOfWeek = config.daysOfWeek.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) },
        )
        val prev = _uiState.value.alarmConfig
        if (prev.hour24 != normalized.hour24 || prev.minute != normalized.minute || !normalized.enabled) {
            lastFiredAlarmKey = null
        }
        prefs.saveAlarmConfig(normalized)
        _uiState.update { it.copy(alarmConfig = normalized) }
    }

    fun triggerAlarmNow(isTest: Boolean = false) {
        touchUserActivity()
        setDimmed(false)
        alarmChimeJob?.cancel()
        alarmSnoozeJob?.cancel()
        _uiState.update { it.copy(activeAlarmRinging = true, alarmSnoozeUntilMs = null) }

        val config = _uiState.value.alarmConfig
        if (config.speakBriefTiming == AlarmBriefTiming.AT_ALARM) {
            val brief = OnDeviceVoiceEngine.handleCommand("Give me my morning briefing", currentVoiceContextSnapshot())
            _uiState.update { it.copy(quickAnswerCard = brief.card ?: it.quickAnswerCard) }
            if (_uiState.value.capabilities.hasAudioOutput) {
                voiceController?.playChime()
                if (_uiState.value.canSpeakAloud) {
                    speakAloud(brief.spokenText)
                }
            }
        } else if (_uiState.value.capabilities.hasAudioOutput) {
            alarmChimeJob = viewModelScope.launch {
                val maxRepeats = if (isTest) 6 else 115
                repeat(maxRepeats) {
                    if (!_uiState.value.activeAlarmRinging) return@launch
                    voiceController?.playChime()
                    delay(2_600L)
                }
            }
        }
    }

    fun dismissActiveAlarm() {
        touchUserActivity()
        alarmChimeJob?.cancel()
        alarmSnoozeJob?.cancel()
        val timing = _uiState.value.alarmConfig.speakBriefTiming
        _uiState.update { it.copy(activeAlarmRinging = false, alarmSnoozeUntilMs = null) }
        if (timing == AlarmBriefTiming.ON_DISMISS) {
            val brief = OnDeviceVoiceEngine.handleCommand("Give me my morning briefing", currentVoiceContextSnapshot())
            _uiState.update { it.copy(quickAnswerCard = brief.card ?: it.quickAnswerCard) }
            if (_uiState.value.canSpeakAloud) {
                speakAloud(brief.spokenText)
            }
        }
    }

    fun snoozeActiveAlarm(minutes: Int = 5) {
        touchUserActivity()
        alarmChimeJob?.cancel()
        alarmSnoozeJob?.cancel()
        voiceController?.stopSpeaking()
        val untilMs = System.currentTimeMillis() + minutes * 60_000L
        _uiState.update { it.copy(activeAlarmRinging = false, alarmSnoozeUntilMs = untilMs) }
        alarmSnoozeJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            triggerAlarmNow()
        }
    }

    private fun handleFinalVoiceTranscript(transcript: String) {
        if (voiceSessionCancelled) return
        touchUserActivity()
        val snapshot = currentVoiceContextSnapshot()

        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    voiceState = VoiceAssistantState.THINKING,
                    voiceUserTranscript = transcript,
                    isFollowUpListening = false,
                    voiceError = null,
                )
            }

            val apiKey = _uiState.value.geminiApiKey
            val result = if (apiKey.isNotBlank()) {
                val imageB64 = if (_uiState.value.capabilities.hasCamera &&
                    GeminiAssistantClient.looksLikeVisionQuestion(transcript)
                ) {
                    cameraController?.captureSnapshotBase64()
                } else {
                    null
                }
                val geminiRes = geminiClient.ask(
                    apiKey = apiKey,
                    userText = transcript,
                    ctx = snapshot,
                    memoryEntries = _uiState.value.memoryEntries,
                    history = conversationHistory.toList(),
                    imageBase64Jpeg = imageB64,
                )
                geminiRes.fold(
                    onSuccess = { resp ->
                        conversationHistory.add(ConversationTurn("user", transcript))
                        conversationHistory.add(ConversationTurn("model", resp.spokenText))
                        com.nernal.frame.data.VoiceTurnResult(
                            spokenText = resp.spokenText,
                            card = resp.displayCard,
                            actions = resp.actions,
                            shouldEndConversation = resp.shouldEndConversation,
                        )
                    },
                    onFailure = { err ->
                        val fallback = OnDeviceVoiceEngine.handleCommand(transcript, snapshot)
                        _uiState.update { it.copy(voiceError = "Gemini unavailable (${err.message}); used on-device voice") }
                        fallback
                    },
                )
            } else {
                OnDeviceVoiceEngine.handleCommand(transcript, snapshot)
            }

            if (voiceSessionCancelled) return@launch

            executeVoiceActions(result.actions)
            _uiState.update {
                it.copy(
                    voiceState = VoiceAssistantState.SPEAKING,
                    voiceAssistantTranscript = result.spokenText,
                    quickAnswerCard = result.card ?: it.quickAnswerCard,
                )
            }

            if (_uiState.value.canSpeakAloud) {
                speakWithBestEngine(text = result.spokenText, showOverlay = true) {
                    if (voiceSessionCancelled) return@speakWithBestEngine
                    if (result.shouldEndConversation) {
                        closeVoiceSession()
                    } else if (_uiState.value.capabilities.hasOnDeviceSpeech) {
                        startVoiceListening(isFollowUp = true)
                    } else {
                        _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
                    }
                }
            } else {
                _uiState.update { it.copy(voiceState = VoiceAssistantState.IDLE) }
            }
        }
    }

    private fun executeVoiceActions(actions: List<VoiceAction>) {
        for (action in actions) {
            when (action) {
                is VoiceAction.AddReminder -> addReminder(
                    text = action.text,
                    category = action.category,
                    explicitDateKey = action.dateKey,
                    explicitTime = action.time,
                )
                is VoiceAction.CompleteReminder -> {
                    val q = action.query.lowercase(Locale.US)
                    val match = _uiState.value.reminders.firstOrNull {
                        !it.completed && it.text.lowercase(Locale.US).contains(q)
                    }
                    if (match != null) toggleReminder(match.id)
                }
                is VoiceAction.DeleteReminder -> {
                    val q = action.query.lowercase(Locale.US)
                    val match = _uiState.value.reminders.firstOrNull {
                        it.text.lowercase(Locale.US).contains(q)
                    }
                    if (match != null) deleteReminder(match.id)
                }
                is VoiceAction.SaveMemory -> {
                    val existingLocal = prefs.loadLocalMemoryEntries().toMutableList()
                    val entry = MemoryEntry(
                        id = "local-${UUID.randomUUID()}",
                        notebook = action.notebook,
                        title = action.title,
                        summary = action.summary,
                        content = action.content,
                        updatedAtMs = System.currentTimeMillis(),
                        fromPi = false,
                    )
                    existingLocal.removeAll {
                        it.title.equals(action.title, ignoreCase = true) &&
                            it.notebook.equals(action.notebook, ignoreCase = true)
                    }
                    existingLocal.add(0, entry)
                    prefs.saveLocalMemoryEntries(existingLocal)
                    val merged = mergeMemoryEntries(existingLocal, _uiState.value.memoryEntries.filter { it.fromPi })
                    _uiState.update {
                        it.copy(
                            memoryEntries = merged,
                            syncNotice = "Saved \"${action.title}\" to ${action.notebook}",
                        )
                    }
                }
                is VoiceAction.DashboardCommand -> when (action.command) {
                    "open_radar" -> openRadarModal()
                    "close_radar" -> closeRadarModal()
                    "next_photo" -> nextPhoto()
                    "previous_photo" -> previousPhoto()
                    "star_photo" -> toggleCurrentStarred()
                    "fullscreen_photos" -> setFullscreen(true)
                    "exit_fullscreen" -> setFullscreen(false)
                    "show_calendar_primary" -> setCalendarPrimaryMode(true)
                    "show_photos" -> setCalendarPrimaryMode(false)
                    "dim_screen" -> setDimmed(true)
                    "wake_screen" -> setDimmed(false)
                }
                VoiceAction.EndConversation -> Unit
            }
        }
    }

    private fun mergeMemoryEntries(local: List<MemoryEntry>, pi: List<MemoryEntry>): List<MemoryEntry> =
        (local + pi)
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAtMs }

    private fun startIdleSleepLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                val s = _uiState.value
                val mins = s.autoSleepMinutes
                if (mins <= 0 || s.isDimmed) continue
                if (s.voiceState != VoiceAssistantState.IDLE) continue
                if (s.showSettingsDialog || s.showAccountDialog || s.showHelpDialog ||
                    s.showFeedbackDialog || s.showRadarModal || s.showPiSyncDialog ||
                    s.showMemoryDialog || s.selectedWeatherDayIso != null
                ) continue
                val elapsedMs = System.currentTimeMillis() - lastUserActivityMs
                if (elapsedMs >= mins * 60_000L) {
                    setDimmed(true)
                }
            }
        }
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
        val dayDateStr = DAY_DATE_FMT.format(now)
        val todayStr = DATE_KEY_FMT.format(now)
        val cur = _uiState.value
        if (cur.clockTime != timeStr || cur.clockAmPm != amPmStr || cur.clockDate != dateStr ||
            cur.clockDayDate != dayDateStr || cur.todayKey != todayStr
        ) {
            _uiState.update {
                it.copy(
                    clockTime = timeStr,
                    clockAmPm = amPmStr,
                    clockDate = dateStr,
                    clockDayDate = dayDateStr,
                    todayKey = todayStr,
                )
            }
        }
        checkUpcomingEventReminders(now)
        val alarm = _uiState.value.alarmConfig
        if (alarm.enabled &&
            now.hour == alarm.hour24 &&
            now.minute == alarm.minute &&
            now.dayOfWeek.value in alarm.daysOfWeek
        ) {
            val fireKey = "$todayStr@${alarm.hour24}:${alarm.minute}"
            if (lastFiredAlarmKey != fireKey) {
                lastFiredAlarmKey = fireKey
                triggerAlarmNow()
            }
        }
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
            if (_uiState.value.isDimmed && !_uiState.value.isManualNightMode) {
                setDimmed(false)
            }
            if (state.capabilities.hasAudioOutput && state.audibleEventAlertsEnabled && !_uiState.value.isManualNightMode) {
                voiceController?.playChime()
                if (state.canSpeakAloud) {
                    val timePart = upcomingEvent.time?.takeIf { it.isNotBlank() }?.let { " at $it" }.orEmpty()
                    speakWithBestEngine(
                        text = "Upcoming event$timePart: ${upcomingEvent.text}",
                        showOverlay = false,
                    )
                }
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
                    val currentAlertKeys = data.alerts.map { it.alertKey() }.toSet()
                    val prunedDismissed = _uiState.value.dismissedAlertKeys.intersect(currentAlertKeys)
                    if (prunedDismissed != _uiState.value.dismissedAlertKeys) {
                        prefs.saveDismissedAlertKeys(prunedDismissed)
                    }
                    _uiState.update {
                        it.copy(
                            weather = data,
                            weatherLoading = false,
                            weatherError = null,
                            dismissedAlertKeys = prunedDismissed,
                        )
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
    // Raspberry Pi Two-Way Sync (Optional Home Hub)
    // -------------------------------------------------------------------------

    fun setPiSyncEnabled(enabled: Boolean) {
        prefs.savePiSyncEnabled(enabled)
        _uiState.update {
            it.copy(
                piSyncEnabled = enabled,
                piConnection = if (enabled) PiConnectionState.CHECKING else PiConnectionState.STANDALONE,
                activePiUrl = if (enabled) it.activePiUrl else null,
            )
        }
        if (enabled) {
            syncWithPiNow()
        }
    }

    private suspend fun runPiSyncCycle(includePhotos: Boolean): Boolean = piSyncMutex.withLock {
        if (!_uiState.value.piSyncEnabled) return@withLock false

        val allRemOps = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        val piOps = allRemOps.filter { it.sourceId == null || it.sourceId == NernalRepository.PI_SOURCE_ID }
        val favOps = withContext(Dispatchers.IO) { prefs.loadPendingFavOps() }

        val outcome = repo.syncWithPi(
            configuredPiUrl = _uiState.value.piBaseUrl,
            pendingReminderOps = piOps,
            pendingFavOps = favOps,
            includePhotos = includePhotos,
            includeMemoryBook = true,
        )

        if (outcome.reachableUrl == null) {
            _uiState.update {
                it.copy(
                    activePiUrl = null,
                    piConnection = PiConnectionState.STANDALONE,
                    pendingSyncCount = allRemOps.size + favOps.size,
                )
            }
            return@withLock false
        }

        val attemptedPiIds = piOps.map { it.opId }.toSet()
        val latestAllOps = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        val preservedOps = latestAllOps.filter { it.opId !in attemptedPiIds }
        val combinedRemOps = outcome.remainingReminderOps + preservedOps

        withContext(Dispatchers.IO) {
            prefs.savePendingReminderOps(combinedRemOps)
            prefs.savePendingFavOps(outcome.remainingFavOps)
        }

        outcome.reminders?.let { piList ->
            val nonPiReminders = _uiState.value.reminders.filter {
                it.sourceId != null && it.sourceId != NernalRepository.PI_SOURCE_ID
            }
            val merged = sortReminders(applyUnflushedReminderOps(piList + nonPiReminders, combinedRemOps))
            withContext(Dispatchers.IO) { prefs.saveReminders(merged) }
            _uiState.update { s -> s.copy(reminders = merged) }
            checkUpcomingEventReminders()
        }

        outcome.starredIds?.let { favs ->
            withContext(Dispatchers.IO) { prefs.saveStarredIds(favs) }
            _uiState.update { s -> s.copy(starredIds = favs) }
        }

        outcome.memoryEntries?.let { piMemories ->
            val localOnly = withContext(Dispatchers.IO) { prefs.loadLocalMemoryEntries() }
            val mergedMem = mergeMemoryEntries(localOnly, piMemories)
            _uiState.update { s -> s.copy(memoryEntries = mergedMem) }
        }

        if (!outcome.albumPhotos.isNullOrEmpty()) {
            val total = outcome.albumTotalCount ?: outcome.albumPhotos.size
            val existingPicked = libraryPool.filter { it.albumUrl == NernalRepository.PICKER_ALBUM_MARKER }
            if (libraryPool.size < outcome.albumPhotos.size + existingPicked.size) {
                val mergedPool = (outcome.albumPhotos + existingPicked).distinctBy { it.id }
                libraryPool = mergedPool
                withContext(Dispatchers.IO) {
                    prefs.saveAlbumLibrary(mergedPool, maxOf(total, mergedPool.size))
                }
            }
            applyLibraryPoolToState(totalCount = maxOf(total, libraryPool.size), syncDone = true)
        }

        _uiState.update {
            it.copy(
                activePiUrl = outcome.reachableUrl,
                piConnection = PiConnectionState.CONNECTED,
                pendingSyncCount = combinedRemOps.size + outcome.remainingFavOps.size,
                lastPiSyncMs = System.currentTimeMillis(),
                syncNotice = null,
            )
        }
        return@withLock true
    }

    fun syncWithPiNow(customUrlInput: String? = null) {
        viewModelScope.launch {
            if (!_uiState.value.piSyncEnabled) {
                prefs.savePiSyncEnabled(true)
            }
            val normalizedUrl = if (customUrlInput != null) {
                prefs.savePiBaseUrl(customUrlInput)
            } else {
                _uiState.value.piBaseUrl
            }
            _uiState.update {
                it.copy(
                    piSyncEnabled = true,
                    piBaseUrl = normalizedUrl,
                    piConnection = PiConnectionState.CHECKING,
                    syncNotice = "Connecting to Pi...",
                )
            }
            val ok = runPiSyncCycle(includePhotos = true)
            _uiState.update {
                it.copy(
                    syncNotice = if (ok) {
                        "Synced with Pi at ${it.activePiUrl}"
                    } else {
                        "Could not reach $normalizedUrl — queued changes stay saved locally"
                    }
                )
            }
        }
    }

    fun rebuildFullPhotoLibrary() {
        viewModelScope.launch {
            val connectedPi = _uiState.value.activePiUrl
            if (connectedPi != null && _uiState.value.piSyncEnabled) {
                _uiState.update { it.copy(albumsSyncing = true, syncNotice = "Asking Pi to rebuild album index...") }
                repo.triggerPiAlbumRefresh(connectedPi)
                delay(4_000L)
                runPiSyncCycle(includePhotos = true)
                _uiState.update { it.copy(albumsSyncing = false, syncNotice = "Refreshed library from Pi") }
            } else {
                withContext(Dispatchers.IO) { prefs.clearAlbumLibrary() }
                refreshPhotoAlbumsOnly()
            }
        }
    }

    fun setShowPiSyncDialog(show: Boolean) {
        _uiState.update { it.copy(showPiSyncDialog = show, syncNotice = if (!show) null else it.syncNotice) }
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
                bringAppToFront()
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
                    bringAppToFront()
                    refreshGoogleMetadataAndSync(forcePhotoRefresh = true)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            authState = GoogleAuthState.ERROR,
                            authError = err.message ?: "Google sign-in failed",
                        )
                    }
                    bringAppToFront()
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

        val keepLocalOrPi = _uiState.value.piSyncEnabled || BuildConfig.IS_NERNAL_FAMILY_BUILD
        val keptReminders = sortReminders(
            _uiState.value.reminders.filter {
                it.sourceId == null || it.sourceId == NernalRepository.PI_SOURCE_ID
            }
        )
        prefs.saveReminders(keptReminders)

        val remainingOps = prefs.loadPendingReminderOps().filter {
            it.sourceId == null || it.sourceId == NernalRepository.PI_SOURCE_ID
        }
        prefs.savePendingReminderOps(remainingOps)
        val pendingCount = remainingOps.size + if (_uiState.value.piSyncEnabled) prefs.loadPendingFavOps().size else 0

        val remainingPhotos = if (keepLocalOrPi || _uiState.value.customSharedAlbumUrls.isNotEmpty()) {
            libraryPool.filterNot { it.albumUrl == NernalRepository.PICKER_ALBUM_MARKER }
        } else {
            emptyList()
        }
        libraryPool = remainingPhotos
        if (remainingPhotos.isNotEmpty()) {
            prefs.saveAlbumLibrary(remainingPhotos, remainingPhotos.size)
        } else {
            prefs.clearAlbumLibrary()
        }
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
                defaultTaskListId = null,
                tasksError = null,
                photosApiError = null,
                pickerSessionUri = null,
                pickerPolling = false,
                reminders = keptReminders,
                allPhotos = if (remainingPhotos.isEmpty()) emptyList() else it.allPhotos,
                activePhotos = if (remainingPhotos.isEmpty()) emptyList() else it.activePhotos,
                libraryTotalCount = remainingPhotos.size,
                currentBitmap = if (remainingPhotos.isEmpty()) null else it.currentBitmap,
                currentPhotoIndex = 0,
                pendingSyncCount = pendingCount,
                recentlyDeletedReminder = null,
                activeEventAlert = null,
                syncNotice = "Signed out of Google",
            )
        }
        if (remainingPhotos.isNotEmpty()) {
            applyLibraryPoolToState(remainingPhotos.size, syncDone = true)
        }
    }

    // -------------------------------------------------------------------------
    // Initial Load & Background Sync Loops
    // -------------------------------------------------------------------------

    private fun startInitialLoadAndSyncLoops() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { prefs.loadAlbumLibrary() }
            if (cached.photos.isNotEmpty()) {
                libraryPool = cached.photos
                applyLibraryPoolToState(cached.totalLibraryCount, syncDone = true)
            }

            // 1. Optional Raspberry Pi two-way sync
            val syncedPi = if (_uiState.value.piSyncEnabled) {
                runPiSyncCycle(includePhotos = true)
            } else {
                false
            }

            // 2. Google account sync if signed in
            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                refreshGoogleMetadataAndSync(forcePhotoRefresh = !syncedPi)
            } else if (!syncedPi && _uiState.value.customSharedAlbumUrls.isNotEmpty() &&
                (cached.photos.isEmpty() || (BuildConfig.IS_NERNAL_FAMILY_BUILD && cached.photos.size < 400))
            ) {
                refreshPhotoAlbumsOnly()
            }

            // 3. Periodic background sync loop
            var tickCount = 0
            while (isActive) {
                val interval = if (_uiState.value.piSyncEnabled) {
                    PI_SYNC_INTERVAL_MS
                } else if (_uiState.value.isDimmed) {
                    GOOGLE_SYNC_DIMMED_INTERVAL_MS
                } else {
                    GOOGLE_SYNC_INTERVAL_MS
                }
                delay(interval)
                tickCount++

                val connectedPi = if (_uiState.value.piSyncEnabled) {
                    val wasOffline = _uiState.value.activePiUrl == null
                    val pullPhotosOnTick = wasOffline || (tickCount % 20 == 0)
                    runPiSyncCycle(includePhotos = pullPhotosOnTick)
                } else {
                    false
                }

                if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                    val googleIntervalTicks = when {
                        !_uiState.value.piSyncEnabled -> 1
                        _uiState.value.isDimmed -> 6
                        else -> 2
                    }
                    if (tickCount % googleIntervalTicks == 0) {
                        val photosExpired = System.currentTimeMillis() - lastPhotosRefreshMs >= PHOTOS_URL_REFRESH_MS
                        syncGoogleData(includePhotos = photosExpired && !connectedPi)
                    }
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
            var defaultList = _uiState.value.defaultTaskListId
                ?: withContext(Dispatchers.IO) { prefs.loadDefaultTaskListId() }
            if (lists.isNotEmpty() && (defaultList == null || lists.none { it.id == defaultList })) {
                defaultList = lists.firstOrNull { it.id in selLists }?.id ?: lists.first().id
                withContext(Dispatchers.IO) { prefs.saveDefaultTaskListId(defaultList) }
            }
            _uiState.update {
                val activeFilters = if (it.activeTaskListFilters.isEmpty() && selLists.isNotEmpty() && prefs.loadActiveTaskListFilters() == null) {
                    selLists
                } else {
                    it.activeTaskListFilters
                }
                it.copy(
                    availableTaskLists = lists,
                    selectedTaskListIds = selLists,
                    defaultTaskListId = defaultList,
                    activeTaskListFilters = activeFilters,
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
        val favOpsCount = if (_uiState.value.piSyncEnabled) withContext(Dispatchers.IO) { prefs.loadPendingFavOps().size } else 0
        val localOrPiReminders = _uiState.value.reminders.filter {
            it.sourceId == null || it.sourceId == NernalRepository.PI_SOURCE_ID
        }
        val mergedReminders = sortReminders(applyUnflushedReminderOps(localOrPiReminders + events + tasks, latestOps))
        val prevReminders = _uiState.value.reminders
        if (mergedReminders != prevReminders) {
            withContext(Dispatchers.IO) { prefs.saveReminders(mergedReminders) }
        }
        Log.d("NernalPerf", "syncGoogleData: synced ${events.size} events and ${tasks.size} tasks (includePhotos=$includePhotos)")
        _uiState.update {
            it.copy(
                reminders = mergedReminders,
                pendingSyncCount = latestOps.size + favOpsCount,
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

    fun setDefaultTaskList(listId: String) {
        if (listId.isBlank()) return
        prefs.saveDefaultTaskListId(listId)
        val cur = _uiState.value
        val needsSync = listId !in cur.selectedTaskListIds
        val nextSelected = if (needsSync) cur.selectedTaskListIds + listId else cur.selectedTaskListIds
        if (needsSync) {
            prefs.saveSelectedTaskListIds(nextSelected)
        }
        val nextActiveFilters = if (cur.activeTaskListFilters.isNotEmpty() && listId !in cur.activeTaskListFilters) {
            (cur.activeTaskListFilters + listId).also { prefs.saveActiveTaskListFilters(it) }
        } else {
            cur.activeTaskListFilters
        }
        _uiState.update {
            it.copy(
                defaultTaskListId = listId,
                selectedTaskListIds = nextSelected,
                activeTaskListFilters = nextActiveFilters,
            )
        }
        if (needsSync && _uiState.value.authState == GoogleAuthState.SIGNED_IN) {
            viewModelScope.launch { syncGoogleData(includePhotos = false) }
        }
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
            .filter { !it.sourceId.isNullOrBlank() && it.sourceId != NernalRepository.PI_SOURCE_ID && !it.remoteId.isNullOrBlank() }
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
            val connectedPi = if (_uiState.value.piSyncEnabled) _uiState.value.activePiUrl else null
            if (libraryPool.size > ROTATION_WINDOW_SIZE) {
                applyLibraryPoolToState(_uiState.value.libraryTotalCount.coerceAtLeast(libraryPool.size), syncDone = true)
            } else if (connectedPi != null) {
                runPiSyncCycle(includePhotos = true)
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
                    state.showAccountDialog || state.showSettingsDialog || state.showPiSyncDialog ||
                    state.showMemoryDialog || state.showInAppAuthWebView || state.selectedWeatherDayIso != null ||
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
                    !latest.showAccountDialog && !latest.showSettingsDialog && !latest.showPiSyncDialog &&
                    !latest.showMemoryDialog && !latest.showInAppAuthWebView && latest.selectedWeatherDayIso == null &&
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
            touchUserActivity()
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
        var nowStarred = false
        _uiState.update { state ->
            val nextIds = state.starredIds.toMutableSet()
            nowStarred = nextIds.add(current.id)
            if (!nowStarred) nextIds.remove(current.id)
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

        if (_uiState.value.piSyncEnabled) {
            val favOps = prefs.loadPendingFavOps().filterNot { it.photoId == current.id }.toMutableList()
            favOps.add(PendingFavOp(current.id, nowStarred))
            prefs.savePendingFavOps(favOps)
            _uiState.update { it.copy(pendingSyncCount = prefs.loadPendingReminderOps().size + favOps.size) }
            viewModelScope.launch { runPiSyncCycle(includePhotos = false) }
        }
    }

    fun dismissWeatherAlert(alert: WeatherAlert) {
        val next = _uiState.value.dismissedAlertKeys + alert.alertKey()
        prefs.saveDismissedAlertKeys(next)
        _uiState.update { it.copy(dismissedAlertKeys = next) }
    }

    fun toggleWeatherCollapsed() {
        val next = !_uiState.value.weatherCollapsed
        prefs.saveWeatherCollapsed(next)
        _uiState.update { it.copy(weatherCollapsed = next) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        if (!show) {
            authManager.stopGeminiKeyServer()
        }
        _uiState.update {
            it.copy(
                showSettingsDialog = show,
                geminiKeyLanUrl = if (show) it.geminiKeyLanUrl else null,
                geminiKeyScanNotice = if (show) it.geminiKeyScanNotice else null,
            )
        }
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
        val state = _uiState.value
        val current = state.activeTaskListFilters.ifEmpty {
            state.selectedTaskListIds.ifEmpty { state.availableTaskLists.map { it.id }.toSet() }
        }
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
        sb.appendLine("piSync=${state.piSyncEnabled} (${state.piConnection}), authState=${state.authState}")
        sb.appendLine("capabilities=cam:${state.capabilities.hasCamera},mic:${state.capabilities.hasMicrophone},spk:${state.capabilities.hasAudioOutput},speech:${state.capabilities.hasOnDeviceSpeech}")
        sb.appendLine("cameraWake=${state.cameraMotionWakeEnabled}, autoSleepMin=${state.autoSleepMinutes}, audibleAlerts=${state.audibleEventAlertsEnabled}, geminiConfigured=${state.geminiApiKey.isNotBlank()}")
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
        touchUserActivity()
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

    fun setNightMode(enabled: Boolean) {
        setDimmed(dimmed = enabled, manualNight = enabled)
    }

    fun setDimmed(dimmed: Boolean, manualNight: Boolean = false) {
        val wasDimmed = _uiState.value.isDimmed
        if (!dimmed) touchUserActivity()
        _uiState.update {
            it.copy(
                isDimmed = dimmed,
                isManualNightMode = if (dimmed) manualNight else false,
            )
        }
        if (wasDimmed && !dimmed) {
            if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                val stale = System.currentTimeMillis() - _uiState.value.lastGoogleSyncMs > GOOGLE_SYNC_INTERVAL_MS
                if (stale) {
                    viewModelScope.launch { syncGoogleData(includePhotos = false) }
                }
            }
            if (_uiState.value.piSyncEnabled && System.currentTimeMillis() - _uiState.value.lastPiSyncMs > PI_SYNC_INTERVAL_MS) {
                viewModelScope.launch { runPiSyncCycle(includePhotos = false) }
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
        touchUserActivity()
        _uiState.update { it.copy(showRadarModal = true) }
        refreshRadar()
    }

    fun closeRadarModal() {
        _uiState.update { it.copy(showRadarModal = false) }
    }

    fun setSelectedWeatherDay(dateIso: String?) {
        if (dateIso != null) touchUserActivity()
        _uiState.update { it.copy(selectedWeatherDayIso = dateIso) }
    }

    fun refreshRadar() {
        viewModelScope.launch {
            _uiState.update { it.copy(radarLoading = true) }
            val loc = _uiState.value.userLocation
            val piUrl = if (_uiState.value.piSyncEnabled) _uiState.value.activePiUrl else null
            repo.fetchRadarData(loc.latitude, loc.longitude, piUrl)
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

    fun setShowTasks(show: Boolean) {
        prefs.saveShowTasks(show)
        _uiState.update { it.copy(showTasks = show) }
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        prefs.saveCalendarViewMode(mode)
        _uiState.update { it.copy(calendarViewMode = mode) }
    }

    fun setLastAddCategory(category: ReminderCategory) {
        prefs.saveLastAddCategory(category)
        _uiState.update { it.copy(lastAddCategory = category) }
    }

    fun setCalendarPrimaryMode(enabled: Boolean) {
        Log.d("NernalClick", "setCalendarPrimaryMode called with enabled=$enabled")
        prefs.saveCalendarPrimaryMode(enabled)
        _uiState.update {
            val nextMode = if (enabled && it.calendarViewMode == CalendarViewMode.NONE) {
                CalendarViewMode.ITINERARY.also(prefs::saveCalendarViewMode)
            } else {
                it.calendarViewMode
            }
            it.copy(
                calendarPrimaryMode = enabled,
                calendarViewMode = nextMode,
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

    private fun resolveTargetSourceId(
        category: ReminderCategory,
        explicitSourceId: String? = null,
    ): String? {
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
                explicitSourceId?.takeIf { it.isNotBlank() }
                    ?: state.defaultTaskListId?.takeIf { id -> state.availableTaskLists.any { it.id == id } }
                    ?: state.availableTaskLists.firstOrNull { it.id in state.selectedTaskListIds }?.id
                    ?: state.selectedTaskListIds.firstOrNull()
                    ?: state.availableTaskLists.firstOrNull()?.id
            }
        }
    }

    fun addReminder(
        text: String,
        category: ReminderCategory,
        explicitDateKey: String? = null,
        explicitTime: String? = null,
        explicitSourceId: String? = null,
    ) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        val today = _uiState.value.todayKey
        val googleTargetId = if (_uiState.value.authState == GoogleAuthState.SIGNED_IN) {
            resolveTargetSourceId(category, explicitSourceId)
        } else null
        val useGoogle = googleTargetId != null
        val usePi = !useGoogle && _uiState.value.piSyncEnabled
        val targetSourceId = googleTargetId ?: if (usePi) NernalRepository.PI_SOURCE_ID else null

        val tempId = "local-${UUID.randomUUID()}"
        val effectiveDateKey = explicitDateKey ?: if (category == ReminderCategory.EVENT) today else null
        val newItem = ReminderItem(
            id = tempId,
            text = cleaned,
            category = category,
            dateKey = effectiveDateKey,
            time = explicitTime,
            completed = false,
            createdAtMs = System.currentTimeMillis(),
            sourceId = targetSourceId,
        )
        var snapshotReminders: List<ReminderItem> = emptyList()
        _uiState.update { state ->
            val updated = sortReminders(listOf(newItem) + state.reminders)
            snapshotReminders = updated
            val nextActiveTaskFilters = if (
                category == ReminderCategory.TASK &&
                googleTargetId != null &&
                state.activeTaskListFilters.isNotEmpty() &&
                googleTargetId !in state.activeTaskListFilters
            ) {
                (state.activeTaskListFilters + googleTargetId).also { prefs.saveActiveTaskListFilters(it) }
            } else {
                state.activeTaskListFilters
            }
            state.copy(reminders = updated, activeTaskListFilters = nextActiveTaskFilters)
        }
        checkUpcomingEventReminders()

        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
            if (useGoogle || usePi) {
                val (opsCount, favCount) = withContext(Dispatchers.IO) {
                    val list = prefs.loadPendingReminderOps().toMutableList()
                    list.add(
                        PendingReminderOp(
                            opId = UUID.randomUUID().toString(),
                            action = PendingReminderAction.ADD,
                            reminderId = tempId,
                            text = newItem.text,
                            time = newItem.time,
                            dateKey = newItem.dateKey,
                            category = category,
                            completed = false,
                            sourceId = targetSourceId,
                        )
                    )
                    prefs.savePendingReminderOps(list)
                    val favs = if (_uiState.value.piSyncEnabled) prefs.loadPendingFavOps().size else 0
                    list.size to favs
                }
                _uiState.update { it.copy(pendingSyncCount = opsCount + favCount) }
                if (useGoogle) {
                    syncGoogleData(includePhotos = false)
                } else if (usePi) {
                    runPiSyncCycle(includePhotos = false)
                }
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
        val isGoogleItem = item.sourceId != null && item.sourceId != NernalRepository.PI_SOURCE_ID
        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveReminders(snapshotReminders) }
            if (isGoogleItem) {
                // Google Tasks supports completion status; Google Calendar events toggle locally only.
                if (item.category != ReminderCategory.EVENT && _uiState.value.authState == GoogleAuthState.SIGNED_IN) {
                    val (opsCount, favCount) = withContext(Dispatchers.IO) {
                        val list = prefs.loadPendingReminderOps().toMutableList()
                        val pendingAddIdx = list.indexOfFirst { it.action == PendingReminderAction.ADD && it.reminderId == id }
                        if (pendingAddIdx >= 0) {
                            list[pendingAddIdx] = list[pendingAddIdx].copy(completed = newCompleted)
                        } else if (!item.remoteId.isNullOrBlank()) {
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
                        val favs = if (_uiState.value.piSyncEnabled) prefs.loadPendingFavOps().size else 0
                        list.size to favs
                    }
                    _uiState.update { it.copy(pendingSyncCount = opsCount + favCount) }
                    syncGoogleData(includePhotos = false)
                }
            } else if (_uiState.value.piSyncEnabled) {
                val (opsCount, favCount) = withContext(Dispatchers.IO) {
                    val list = prefs.loadPendingReminderOps().toMutableList()
                    val pendingAddIdx = list.indexOfFirst { it.action == PendingReminderAction.ADD && it.reminderId == id }
                    if (pendingAddIdx >= 0) {
                        list[pendingAddIdx] = list[pendingAddIdx].copy(completed = newCompleted)
                    } else {
                        list.removeAll { it.action == PendingReminderAction.UPDATE && it.reminderId == id }
                        list.add(
                            PendingReminderOp(
                                opId = UUID.randomUUID().toString(),
                                action = PendingReminderAction.UPDATE,
                                reminderId = id,
                                category = item.category,
                                completed = newCompleted,
                                sourceId = NernalRepository.PI_SOURCE_ID,
                                remoteId = item.remoteId,
                            )
                        )
                    }
                    prefs.savePendingReminderOps(list)
                    list.size to prefs.loadPendingFavOps().size
                }
                _uiState.update { it.copy(pendingSyncCount = opsCount + favCount) }
                runPiSyncCycle(includePhotos = false)
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
        val isGoogleItem = existing.sourceId != null && existing.sourceId != NernalRepository.PI_SOURCE_ID
        if (isGoogleItem) {
            if (_uiState.value.authState != GoogleAuthState.SIGNED_IN) return
            val (opsCount, favCount) = withContext(Dispatchers.IO) {
                val list = prefs.loadPendingReminderOps().toMutableList()
                val wasUnsyncedLocal = list.any { it.action == PendingReminderAction.ADD && it.reminderId == existing.id }
                list.removeAll { it.reminderId == existing.id }
                if (!wasUnsyncedLocal && !existing.remoteId.isNullOrBlank()) {
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
                val favs = if (_uiState.value.piSyncEnabled) prefs.loadPendingFavOps().size else 0
                list.size to favs
            }
            _uiState.update { it.copy(pendingSyncCount = opsCount + favCount) }
            syncGoogleData(includePhotos = false)
        } else if (_uiState.value.piSyncEnabled) {
            val (opsCount, favCount) = withContext(Dispatchers.IO) {
                val list = prefs.loadPendingReminderOps().toMutableList()
                val wasUnsyncedLocal = list.any { it.action == PendingReminderAction.ADD && it.reminderId == existing.id }
                list.removeAll { it.reminderId == existing.id }
                if (!wasUnsyncedLocal) {
                    list.add(
                        PendingReminderOp(
                            opId = UUID.randomUUID().toString(),
                            action = PendingReminderAction.DELETE,
                            reminderId = existing.id,
                            category = existing.category,
                            sourceId = NernalRepository.PI_SOURCE_ID,
                            remoteId = existing.remoteId,
                        )
                    )
                }
                prefs.savePendingReminderOps(list)
                list.size to prefs.loadPendingFavOps().size
            }
            _uiState.update { it.copy(pendingSyncCount = opsCount + favCount) }
            runPiSyncCycle(includePhotos = false)
        }
    }

    private suspend fun flushPendingReminderOps(accessToken: String): List<PendingReminderOp> {
        val allPending = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        val googlePending = allPending.filter { it.sourceId != null && it.sourceId != NernalRepository.PI_SOURCE_ID }
        if (googlePending.isEmpty()) return allPending

        val remainingGoogle = mutableListOf<PendingReminderOp>()
        val tempRemoteMap = mutableMapOf<String, Pair<String, String>>()
        val zone = currentZoneId()
        var halted = false
        var skippedNotice: String? = null

        for (op in googlePending) {
            if (halted) {
                remainingGoogle.add(op)
                continue
            }
            val mapped = tempRemoteMap[op.reminderId]
            val srcId = mapped?.first ?: op.sourceId
            val remId = mapped?.second ?: op.remoteId

            when (op.action) {
                PendingReminderAction.ADD -> {
                    val targetSource = srcId ?: resolveTargetSourceId(op.category)
                    if (targetSource.isNullOrBlank()) {
                        remainingGoogle.add(op)
                        continue
                    }
                    when (
                        val res = if (op.category == ReminderCategory.EVENT) {
                            repo.createCalendarEvent(accessToken, targetSource, op.text, op.dateKey, zone)
                        } else {
                            repo.createTask(accessToken, targetSource, op.text, op.category, op.dateKey)
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
                                            remainingGoogle.add(
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
                            remainingGoogle.add(op)
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
                            remainingGoogle.add(op.copy(sourceId = srcId, remoteId = remId))
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
                            remainingGoogle.add(op.copy(sourceId = srcId, remoteId = remId))
                        }
                        is RemoteMutationResult.PermanentError -> {
                            Log.w("NernalSync", "Dropped poison DELETE op (${op.reminderId}): HTTP ${res.httpCode} ${res.message}")
                            skippedNotice = "Skipped rejected sync item: ${res.message}"
                        }
                    }
                }
            }
        }

        val attemptedIds = googlePending.map { it.opId }.toSet()
        val latestAll = withContext(Dispatchers.IO) { prefs.loadPendingReminderOps() }
        val nonGoogleOrConcurrent = latestAll.filter { it.opId !in attemptedIds }
        val finalRemaining = nonGoogleOrConcurrent + remainingGoogle
        if (finalRemaining != latestAll) {
            withContext(Dispatchers.IO) { prefs.savePendingReminderOps(finalRemaining) }
        }
        if (skippedNotice != null) {
            _uiState.update { it.copy(syncNotice = skippedNotice) }
        }
        return finalRemaining
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
        const val PI_SYNC_INTERVAL_MS = 45_000L
        const val GOOGLE_SYNC_INTERVAL_MS = 60_000L
        const val GOOGLE_SYNC_DIMMED_INTERVAL_MS = 5 * 60_000L
        const val PHOTOS_URL_REFRESH_MS = 45 * 60_000L
        const val ROTATION_WINDOW_SIZE = 400

        val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
        val AMPM_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("a", Locale.US)
        val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)
        val DAY_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
        val DATE_KEY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    }
}
