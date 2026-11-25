package com.nernal.frame

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nernal.frame.data.AlarmBriefTiming
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.VoiceAssistantState
import com.nernal.frame.data.buildWeatherAssessment
import com.nernal.frame.data.parseTimeLabelToMinutes
import com.nernal.frame.data.selectNextUpcomingEvent
import com.nernal.frame.ui.CalendarPrimaryWing
import com.nernal.frame.ui.DimOverlay
import com.nernal.frame.ui.DopplerRadarModal
import com.nernal.frame.ui.EventDetailsDialog
import com.nernal.frame.ui.FeedbackDialog
import com.nernal.frame.ui.FullscreenSlideshowOverlay
import com.nernal.frame.ui.GoogleAccountDialog
import com.nernal.frame.ui.InAppOAuthWebViewDialog
import com.nernal.frame.ui.InformationalWing
import com.nernal.frame.ui.LifestyleWing
import com.nernal.frame.ui.MemoryBookDialog
import com.nernal.frame.ui.NernalColors
import com.nernal.frame.ui.NernalTheme
import com.nernal.frame.ui.PiSyncDialog
import com.nernal.frame.ui.QuickAnswerCardOverlay
import com.nernal.frame.ui.SettingsDialog
import com.nernal.frame.ui.UserGuideDialog
import com.nernal.frame.ui.VoiceSubtitlesOverlay
import com.nernal.frame.ui.WeatherDetailDialog
import com.nernal.frame.ui.openDeviceWifiSettings
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val viewModel: NernalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.isDimmed, state.dimBrightness, state.globalBrightness) {
                val attrs = window.attributes
                attrs.screenBrightness = if (state.isDimmed) {
                    state.dimBrightness
                } else {
                    state.globalBrightness
                }
                window.attributes = attrs
            }

            NernalTheme {
                NernalFrameRoot(
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForegroundChanged(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onForegroundChanged(false)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.touchUserActivity()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun NernalFrameRoot(
    state: NernalUiState,
    viewModel: NernalViewModel,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionsUpdated()
    }

    var askedInitialPermissions by remember { mutableStateOf(false) }
    LaunchedEffect(state.capabilities) {
        if (askedInitialPermissions) return@LaunchedEffect
        val toRequest = buildList {
            if (state.capabilities.hasCamera &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }
            if (state.capabilities.hasMicrophone &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (toRequest.isNotEmpty()) {
            askedInitialPermissions = true
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    LaunchedEffect(state.needsAudioPermissionPrompt, state.needsCameraPermissionPrompt) {
        val toRequest = buildList {
            if (state.needsAudioPermissionPrompt) add(Manifest.permission.RECORD_AUDIO)
            if (state.needsCameraPermissionPrompt) add(Manifest.permission.CAMERA)
        }
        if (toRequest.isNotEmpty()) {
            viewModel.clearPermissionPrompts()
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    val hasOverlayOrBackTarget = state.activeAlarmRinging ||
        state.isDimmed ||
        state.voiceState != VoiceAssistantState.IDLE ||
        state.quickAnswerCard != null ||
        state.showInAppAuthWebView ||
        state.showMemoryDialog ||
        state.showHelpDialog ||
        state.showFeedbackDialog ||
        state.showSettingsDialog ||
        state.showAccountDialog ||
        state.showPiSyncDialog ||
        state.showUpdateDialog ||
        state.selectedEventDetail != null ||
        state.selectedWeatherDayIso != null ||
        state.showRadarModal ||
        state.fullscreenMode ||
        state.calendarPrimaryMode

    BackHandler(enabled = BuildConfig.IS_NERNAL_FAMILY_BUILD || hasOverlayOrBackTarget) {
        when {
            state.activeAlarmRinging -> viewModel.dismissActiveAlarm()
            state.isDimmed -> viewModel.setDimmed(false)
            state.voiceState != VoiceAssistantState.IDLE -> viewModel.closeVoiceSession()
            state.quickAnswerCard != null -> viewModel.dismissQuickAnswerCard()
            state.showInAppAuthWebView -> viewModel.dismissInAppAuthWebView()
            state.showMemoryDialog -> viewModel.setShowMemoryDialog(false)
            state.showHelpDialog -> viewModel.setShowHelpDialog(false)
            state.showFeedbackDialog -> viewModel.setShowFeedbackDialog(false)
            state.showSettingsDialog -> viewModel.setShowSettingsDialog(false)
            state.showAccountDialog -> viewModel.setShowAccountDialog(false)
            state.showPiSyncDialog -> viewModel.setShowPiSyncDialog(false)
            state.showUpdateDialog -> viewModel.dismissUpdateDialog()
            state.selectedEventDetail != null -> viewModel.setSelectedEventDetail(null)
            state.selectedWeatherDayIso != null -> viewModel.setSelectedWeatherDay(null)
            state.showRadarModal -> viewModel.closeRadarModal()
            state.fullscreenMode -> viewModel.setFullscreen(false)
            state.calendarPrimaryMode -> viewModel.setCalendarPrimaryMode(false)
        }
    }

    LaunchedEffect(state.fullscreenMode) {
        if (state.fullscreenMode) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    val nowMinutes = remember(state.clockTime, state.clockAmPm) {
        parseTimeLabelToMinutes("${state.clockTime} ${state.clockAmPm}")
    }
    val nextUpcoming = remember(state.reminders, state.todayKey, nowMinutes) {
        selectNextUpcomingEvent(state.reminders, state.todayKey, nowMinutes)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NernalColors.Bg)
    ) {
        if (state.calendarPrimaryMode) {
            CalendarPrimaryWing(
                state = state,
                onOpenRadar = viewModel::openRadarModal,
                onWeatherDayClick = viewModel::setSelectedWeatherDay,
                onDismissWeatherAlert = viewModel::dismissWeatherAlert,
                onToggleWeatherCollapsed = viewModel::toggleWeatherCollapsed,
                onFilterChange = viewModel::setHubFilter,
                onToggleActiveTaskListFilter = viewModel::toggleActiveTaskListFilter,
                onSetDefaultTaskList = viewModel::setDefaultTaskList,
                onAddReminder = { text, cat, dateKey, taskListId ->
                    viewModel.addReminder(
                        text = text,
                        category = cat,
                        explicitDateKey = dateKey,
                        explicitSourceId = taskListId,
                    )
                },
                onToggleReminder = viewModel::toggleReminder,
                onDeleteReminder = viewModel::deleteReminder,
                onEventClick = viewModel::setSelectedEventDetail,
                onCalendarViewModeChange = viewModel::setCalendarViewMode,
                onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                onOpenAccount = { viewModel.setShowAccountDialog(true) },
                onOpenPiSync = { viewModel.setShowPiSyncDialog(true) },
                onSpeakClick = viewModel::onSpeakButtonClicked,
                onToggleCameraWake = { viewModel.setCameraMotionWakeEnabled(!state.cameraMotionWakeEnabled) },
                onOpenMemories = { viewModel.setShowMemoryDialog(true) },
                onDimScreen = { viewModel.setNightMode(true) },
                onOpenWifiSettings = { openDeviceWifiSettings(context) },
                onToggleCalendarPrimary = viewModel::setCalendarPrimaryMode,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InformationalWing(
                    state = state,
                    onOpenRadar = viewModel::openRadarModal,
                    onWeatherDayClick = viewModel::setSelectedWeatherDay,
                    onDismissWeatherAlert = viewModel::dismissWeatherAlert,
                    onToggleWeatherCollapsed = viewModel::toggleWeatherCollapsed,
                    onFilterChange = viewModel::setHubFilter,
                    onToggleActiveTaskListFilter = viewModel::toggleActiveTaskListFilter,
                    onSetDefaultTaskList = viewModel::setDefaultTaskList,
                    onAddReminder = { text, cat, dateKey, taskListId ->
                        viewModel.addReminder(
                            text = text,
                            category = cat,
                            explicitDateKey = dateKey,
                            explicitSourceId = taskListId,
                        )
                    },
                    onToggleReminder = viewModel::toggleReminder,
                    onDeleteReminder = viewModel::deleteReminder,
                    onEventClick = viewModel::setSelectedEventDetail,
                    onCalendarViewModeChange = viewModel::setCalendarViewMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                LifestyleWing(
                    state = state,
                    onPreviousPhoto = viewModel::previousPhoto,
                    onNextPhoto = viewModel::nextPhoto,
                    onToggleAutoAdvance = viewModel::toggleAutoAdvance,
                    onToggleStar = viewModel::toggleCurrentStarred,
                    onToggleFavoritesFilter = viewModel::toggleFavoritesOnly,
                    onResetFilters = viewModel::resetPhotoFilters,
                    onRefreshAlbums = viewModel::refreshOrReshufflePhotos,
                    onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                    onOpenAccount = { viewModel.setShowAccountDialog(true) },
                    onOpenPiSync = { viewModel.setShowPiSyncDialog(true) },
                    onSpeakClick = viewModel::onSpeakButtonClicked,
                    onToggleCameraWake = { viewModel.setCameraMotionWakeEnabled(!state.cameraMotionWakeEnabled) },
                    onOpenMemories = { viewModel.setShowMemoryDialog(true) },
                    onEnterFullscreen = { viewModel.setFullscreen(true) },
                    onDimScreen = { viewModel.setNightMode(true) },
                    onInspectPhoto = { viewModel.inspectPhotoTemporary(10) },
                    onToggleCalendarPrimary = viewModel::setCalendarPrimaryMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        if (state.fullscreenMode) {
            FullscreenSlideshowOverlay(
                state = state,
                onPrev = viewModel::previousPhoto,
                onNext = viewModel::nextPhoto,
                onToggleStar = viewModel::toggleCurrentStarred,
                onShowAll = viewModel::resetPhotoFilters,
                onRetry = viewModel::refreshOrReshufflePhotos,
                onOpenAccount = {
                    viewModel.setFullscreen(false)
                    viewModel.setShowAccountDialog(true)
                },
                onExit = { viewModel.setFullscreen(false) },
            )
        }

        if (state.showRadarModal) {
            val zoneId = runCatching { ZoneId.of(state.userLocation.timezone) }
                .getOrDefault(NernalRepository.EASTERN_ZONE)
            DopplerRadarModal(
                homeLat = state.userLocation.latitude,
                homeLon = state.userLocation.longitude,
                locationLabel = state.userLocation.name,
                zoneId = zoneId,
                radarData = state.radarData,
                isLoading = state.radarLoading,
                loadTile = viewModel::loadRadarTile,
                onRefresh = viewModel::refreshRadar,
                onDismiss = viewModel::closeRadarModal,
            )
        }

        if (state.showSettingsDialog) {
            SettingsDialog(
                state = state,
                onSlideIntervalChange = viewModel::setSlideIntervalSec,
                onTogglePhotoYear = viewModel::togglePhotoYear,
                onToggleAllPhotoYears = viewModel::toggleAllPhotoYears,
                onAlbumFilterChange = viewModel::setPhotoAlbumFilter,
                onDimBrightnessChange = viewModel::setDimBrightness,
                onGlobalBrightnessChange = viewModel::setGlobalBrightness,
                onWakeOnEventReminderChange = viewModel::setWakeOnEventReminder,
                onCameraMotionWakeChange = viewModel::setCameraMotionWakeEnabled,
                onAutoSleepMinutesChange = viewModel::setAutoSleepMinutes,
                onAudibleEventAlertsChange = viewModel::setAudibleEventAlertsEnabled,
                onSaveGeminiApiKey = viewModel::saveGeminiApiKey,
                onStartGeminiKeyLan = viewModel::startGeminiKeyLanReceiver,
                onStopGeminiKeyLan = viewModel::stopGeminiKeyLanReceiver,
                onScanGeminiKeyTasks = viewModel::scanTasksForGeminiKeys,
                onImportGeminiKeyCandidate = viewModel::importGeminiKeyCandidate,
                onTestAudio = viewModel::testAudioChimeAndVoice,
                onUpdateAlarmConfig = viewModel::updateAlarmConfig,
                onTestAlarm = {
                    viewModel.setShowSettingsDialog(false)
                    viewModel.triggerAlarmNow(isTest = true)
                },
                onPiSyncEnabledChange = viewModel::setPiSyncEnabled,
                onOpenPiSync = {
                    viewModel.setShowSettingsDialog(false)
                    viewModel.setShowPiSyncDialog(true)
                },
                onCalendarViewModeChange = viewModel::setCalendarViewMode,
                onCalendarPrimaryModeChange = viewModel::setCalendarPrimaryMode,
                onOpenWifiSettings = {
                    viewModel.setShowSettingsDialog(false)
                    openDeviceWifiSettings(context)
                },
                onRefreshAlbums = viewModel::refreshOrReshufflePhotos,
                onRemoveCustomSharedUrl = viewModel::removeCustomSharedAlbumUrl,
                onOpenGoogleAccount = {
                    viewModel.setShowSettingsDialog(false)
                    viewModel.setShowAccountDialog(true)
                },
                onOpenHelp = {
                    viewModel.setShowSettingsDialog(false)
                    viewModel.setShowHelpDialog(true)
                },
                onCheckForUpdates = { viewModel.checkForUpdates(manual = true) },
                onInstallUpdate = { viewModel.downloadAndInstallUpdate(context) },
                onResetFilters = viewModel::resetPhotoFilters,
                onDismiss = { viewModel.setShowSettingsDialog(false) },
            )
        }

        if (state.showPiSyncDialog) {
            PiSyncDialog(
                state = state,
                onSyncNow = viewModel::syncWithPiNow,
                onRebuildLibrary = viewModel::rebuildFullPhotoLibrary,
                onDismiss = { viewModel.setShowPiSyncDialog(false) },
            )
        }

        if (state.showMemoryDialog) {
            MemoryBookDialog(
                entries = state.memoryEntries,
                canSpeak = state.capabilities.hasAudioOutput,
                onReadAloud = viewModel::speakAloud,
                onDeleteLocal = viewModel::deleteLocalMemoryEntry,
                onDismiss = { viewModel.setShowMemoryDialog(false) },
            )
        }

        if (state.showAccountDialog) {
            GoogleAccountDialog(
                state = state,
                onStartSignIn = viewModel::startGoogleSignIn,
                onSubmitCodeOrUrl = viewModel::completeGoogleSignIn,
                onSignOut = viewModel::signOutGoogle,
                onSaveOAuthConfig = viewModel::updateOAuthClientConfig,
                onSyncNow = viewModel::syncGoogleNow,
                onToggleCalendar = viewModel::toggleCalendarSelection,
                onToggleTaskList = viewModel::toggleTaskListSelection,
                onSetDefaultTaskList = viewModel::setDefaultTaskList,
                onCreateTaskList = viewModel::createGoogleTaskList,
                onLaunchPicker = viewModel::launchGooglePhotosPicker,
                onAddCustomSharedUrl = viewModel::addCustomSharedAlbumUrl,
                onRemoveCustomSharedUrl = viewModel::removeCustomSharedAlbumUrl,
                onSearchLocations = viewModel::searchLocations,
                onSelectLocation = viewModel::selectUserLocation,
                onAutoDetectLocation = viewModel::autoDetectLocation,
                onDismiss = { viewModel.setShowAccountDialog(false) },
            )
        }

        val activeUrl = state.activeAuthUrl
        if (state.showInAppAuthWebView && activeUrl != null) {
            InAppOAuthWebViewDialog(
                authUrl = activeUrl,
                onRedirectCaptured = viewModel::completeGoogleSignIn,
                onDismiss = viewModel::dismissInAppAuthWebView,
            )
        }

        val updateRelease = state.updateRelease
        if (state.showUpdateDialog && updateRelease != null) {
            com.nernal.frame.ui.AppUpdateDialog(
                release = updateRelease,
                state = state,
                onInstall = { viewModel.downloadAndInstallUpdate(context) },
                onDismiss = viewModel::dismissUpdateDialog,
            )
        }

        val selectedWeatherIso = state.selectedWeatherDayIso
        val weatherSnapshot = state.weather
        if (selectedWeatherIso != null && weatherSnapshot != null) {
            WeatherDetailDialog(
                weather = weatherSnapshot,
                selectedDateIso = selectedWeatherIso,
                locationName = state.userLocation.name,
                activeAlerts = state.activeWeatherAlerts,
                canReadAloud = state.capabilities.hasAudioOutput,
                onSelectDay = viewModel::setSelectedWeatherDay,
                onOpenRadar = {
                    viewModel.setSelectedWeatherDay(null)
                    viewModel.openRadarModal()
                },
                onReadAloud = { day ->
                    val isToday = day.dateIso == weatherSnapshot.daily.firstOrNull()?.dateIso ||
                        day.dayLabel.equals("Today", ignoreCase = true)
                    viewModel.speakAloud(buildWeatherAssessment(weatherSnapshot, day, isToday))
                },
                onDismiss = { viewModel.setSelectedWeatherDay(null) },
            )
        }

        state.selectedEventDetail?.let { eventItem ->
            EventDetailsDialog(
                item = eventItem,
                availableCalendars = state.availableCalendars,
                canReadAloud = state.capabilities.hasAudioOutput,
                onReadAloud = {
                    val datePart = eventItem.dateKey?.let { " on $it" }.orEmpty()
                    val timePart = eventItem.time?.let { " at $it" }.orEmpty()
                    val notesPart = eventItem.notes?.takeIf { it.isNotBlank() }?.let { ". $it" }.orEmpty()
                    viewModel.speakAloud("${eventItem.text}$datePart$timePart$notesPart")
                },
                onDismiss = { viewModel.setSelectedEventDetail(null) },
            )
        }

        state.activeEventAlert?.let { alert ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    color = Color(0xFFD97706),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable {
                            viewModel.setSelectedEventDetail(alert)
                            viewModel.dismissEventAlert()
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Event,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "UPCOMING EVENT (${alert.time ?: "Soon"})",
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                )
                                Text(
                                    text = alert.text,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                        Button(
                            onClick = viewModel::dismissEventAlert,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("Dismiss", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val deletedItem = state.recentlyDeletedReminder
        AnimatedVisibility(
            visible = deletedItem != null && !state.isDimmed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        ) {
            if (deletedItem != null) {
                Surface(
                    color = NernalColors.Card,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Removed \"${deletedItem.text}\"",
                            color = NernalColors.Ink,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Button(
                            onClick = viewModel::undoDeleteReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Accent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Undo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (state.showHelpDialog) {
            UserGuideDialog(
                onOpenFeedback = {
                    viewModel.setShowHelpDialog(false)
                    viewModel.setShowFeedbackDialog(true)
                },
                onDismiss = { viewModel.setShowHelpDialog(false) },
            )
        }

        if (state.showFeedbackDialog) {
            FeedbackDialog(
                state = state,
                onSubmit = viewModel::submitUserFeedback,
                onDismiss = { viewModel.setShowFeedbackDialog(false) },
            )
        }

        VoiceSubtitlesOverlay(
            state = state,
            onStopAndClose = viewModel::closeVoiceSession,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        QuickAnswerCardOverlay(
            card = state.quickAnswerCard,
            canSpeak = state.capabilities.hasAudioOutput,
            isSpeaking = state.voiceState == VoiceAssistantState.SPEAKING,
            onReadAloud = viewModel::speakAloud,
            onStopSpeaking = viewModel::closeVoiceSession,
            onDismiss = viewModel::dismissQuickAnswerCard,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state.isDimmed) {
            DimOverlay(
                time = state.clockTime,
                amPm = state.clockAmPm,
                weather = state.weather,
                nextEvent = nextUpcoming,
                todayKey = state.todayKey,
                cameraWakeActive = state.capabilities.hasCamera && state.cameraMotionWakeEnabled,
                isManualNightMode = state.isManualNightMode,
                alarmConfig = state.alarmConfig,
                onWake = { viewModel.setDimmed(false) },
            )
        }

        if (state.activeAlarmRinging) {
            AlarmRingingOverlay(
                time = state.clockTime,
                amPm = state.clockAmPm,
                label = state.alarmConfig.label,
                speakBriefOnDismiss = state.alarmConfig.speakBriefTiming == AlarmBriefTiming.ON_DISMISS,
                onSnooze = { viewModel.snoozeActiveAlarm(5) },
                onDismiss = viewModel::dismissActiveAlarm,
            )
        }
    }
}

@Composable
private fun AlarmRingingOverlay(
    time: String,
    amPm: String,
    label: String,
    speakBriefOnDismiss: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, NernalColors.Gold),
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = NernalColors.Gold,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = label.uppercase(),
                    color = NernalColors.Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = "$time $amPm",
                    color = NernalColors.Ink,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedButton(
                        onClick = onSnooze,
                        border = BorderStroke(1.5.dp, NernalColors.Border),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Text("Snooze 5m", color = NernalColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Text(
                            text = if (speakBriefOnDismiss) "Silence & Start Day" else "Dismiss",
                            color = NernalColors.Bg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
