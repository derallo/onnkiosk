package com.nernal.frame

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.parseTimeLabelToMinutes
import com.nernal.frame.data.selectNextUpcomingEvent
import com.nernal.frame.ui.CalendarPrimaryWing
import com.nernal.frame.ui.DimOverlay
import com.nernal.frame.ui.DopplerRadarModal
import com.nernal.frame.ui.EventDetailsDialog
import com.nernal.frame.ui.FullscreenSlideshowOverlay
import com.nernal.frame.ui.GoogleAccountDialog
import com.nernal.frame.ui.InAppOAuthWebViewDialog
import com.nernal.frame.ui.InformationalWing
import com.nernal.frame.ui.LifestyleWing
import com.nernal.frame.ui.NernalColors
import com.nernal.frame.ui.NernalTheme
import com.nernal.frame.ui.SettingsDialog
import com.nernal.frame.ui.UserGuideDialog
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
                onRefreshWeather = viewModel::refreshWeather,
                onOpenRadar = viewModel::openRadarModal,
                onFilterChange = viewModel::setHubFilter,
                onToggleActiveTaskListFilter = viewModel::toggleActiveTaskListFilter,
                onAddReminder = viewModel::addReminder,
                onToggleReminder = viewModel::toggleReminder,
                onDeleteReminder = viewModel::deleteReminder,
                onEventClick = viewModel::setSelectedEventDetail,
                onCalendarViewModeChange = viewModel::setCalendarViewMode,
                onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                onOpenHelp = { viewModel.setShowHelpDialog(true) },
                onOpenAccount = { viewModel.setShowAccountDialog(true) },
                onDimScreen = { viewModel.setDimmed(true) },
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
                    onRefreshWeather = viewModel::refreshWeather,
                    onOpenRadar = viewModel::openRadarModal,
                    onFilterChange = viewModel::setHubFilter,
                    onToggleActiveTaskListFilter = viewModel::toggleActiveTaskListFilter,
                    onAddReminder = viewModel::addReminder,
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
                    onOpenHelp = { viewModel.setShowHelpDialog(true) },
                    onOpenAccount = { viewModel.setShowAccountDialog(true) },
                    onEnterFullscreen = { viewModel.setFullscreen(true) },
                    onDimScreen = { viewModel.setDimmed(true) },
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
                onCalendarViewModeChange = viewModel::setCalendarViewMode,
                onCalendarPrimaryModeChange = viewModel::setCalendarPrimaryMode,
                onOpenWifiSettings = {
                    viewModel.setShowSettingsDialog(false)
                    openDeviceWifiSettings(context)
                },
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

        state.selectedEventDetail?.let { eventItem ->
            EventDetailsDialog(
                item = eventItem,
                availableCalendars = state.availableCalendars,
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
                onDismiss = { viewModel.setShowHelpDialog(false) },
            )
        }

        if (state.isDimmed) {
            DimOverlay(
                time = state.clockTime,
                amPm = state.clockAmPm,
                weather = state.weather,
                nextEvent = nextUpcoming,
                todayKey = state.todayKey,
                onWake = { viewModel.setDimmed(false) },
            )
        }
    }
}
