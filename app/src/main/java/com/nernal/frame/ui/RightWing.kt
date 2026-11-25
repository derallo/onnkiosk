package com.nernal.frame.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LastPage
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.BuildConfig
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.AlarmBriefTiming
import com.nernal.frame.data.AlarmConfig
import com.nernal.frame.data.AlbumPhoto
import com.nernal.frame.data.CalendarViewMode
import com.nernal.frame.data.GeminiKeyCandidate
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.PiConnectionState
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.VoiceAssistantState
import com.nernal.frame.data.WeatherData
import kotlinx.coroutines.delay

fun openDeviceWifiSettings(context: Context) {
    openDeviceSystemSettings(context, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)
}

fun openDeviceHomeSettings(context: Context) {
    openDeviceSystemSettings(context, Settings.ACTION_HOME_SETTINGS, Settings.ACTION_SETTINGS)
}

private fun openDeviceSystemSettings(
    context: Context,
    primaryAction: String,
    fallbackAction: String,
) {
    val launchedPrimary = runCatching {
        context.startActivity(
            Intent(primaryAction).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
        )
    }.isSuccess
    if (!launchedPrimary) {
        runCatching {
            context.startActivity(
                Intent(fallbackAction).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            )
        }
    }
}

@Composable
fun LifestyleWing(
    state: NernalUiState,
    onPreviousPhoto: () -> Unit,
    onNextPhoto: () -> Unit,
    onToggleAutoAdvance: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onResetFilters: () -> Unit,
    onRefreshAlbums: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenPiSync: () -> Unit = {},
    onSpeakClick: () -> Unit = {},
    onToggleCameraWake: () -> Unit = {},
    onOpenMemories: () -> Unit = {},
    onEnterFullscreen: () -> Unit,
    onDimScreen: () -> Unit,
    onInspectPhoto: () -> Unit,
    onToggleCalendarPrimary: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top Utility Bar: Speak (outlined) on left; right-to-left from right edge: Settings, Night, Eye (if camera)
        // Account & Wi-Fi appear only during first setup (always accessible inside Settings afterward)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // LEFT: Speak (outlined), Memories (if active), Pi Sync (if enabled), and Account/Wi-Fi only on first setup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.capabilities.hasVoiceInputAndOutput) {
                    val isVoiceActive = state.voiceState != VoiceAssistantState.IDLE
                    val voiceBg = when (state.voiceState) {
                        VoiceAssistantState.LISTENING -> Color(0xFF0E3A2C)
                        VoiceAssistantState.THINKING -> Color(0xFF172554)
                        VoiceAssistantState.SPEAKING -> Color(0xFF33260F)
                        VoiceAssistantState.IDLE -> NernalColors.Card
                    }
                    val voiceAccent = when (state.voiceState) {
                        VoiceAssistantState.LISTENING -> NernalColors.Emerald
                        VoiceAssistantState.THINKING -> NernalColors.Accent
                        VoiceAssistantState.SPEAKING -> NernalColors.Amber
                        VoiceAssistantState.IDLE -> NernalColors.Gold
                    }
                    Surface(
                        color = voiceBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, voiceAccent),
                        modifier = Modifier
                            .width(72.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSpeakClick),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVoiceActive) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = if (isVoiceActive) "Stop Voice Assistant" else "Speak",
                                tint = voiceAccent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                if (state.memoryEntries.isNotEmpty() || state.geminiApiKey.isNotBlank() || state.piSyncEnabled) {
                    Surface(
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier
                            .size(width = 46.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenMemories),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = "Memory Book",
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }

                if (state.piSyncEnabled) {
                    val (piDotColor, piLabel) = when {
                        state.pendingSyncCount > 0 -> NernalColors.Amber to "Queue (${state.pendingSyncCount})"
                        state.piConnection == PiConnectionState.CONNECTED -> NernalColors.Emerald to "Pi Linked"
                        state.piConnection == PiConnectionState.CHECKING -> NernalColors.WarmAmber to "Pi..."
                        else -> NernalColors.InkMuted to "Pi"
                    }
                    Surface(
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPiSync),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(piDotColor),
                            )
                            Text(
                                text = piLabel,
                                color = NernalColors.Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (state.showFirstSetupShortcuts) {
                    val dotColor = when {
                        state.authState == GoogleAuthState.SIGNED_IN && state.googleSyncing -> NernalColors.WarmAmber
                        state.authState == GoogleAuthState.SIGNED_IN -> NernalColors.Emerald
                        state.authState == GoogleAuthState.SIGNING_IN -> NernalColors.Accent
                        state.authState == GoogleAuthState.ERROR -> NernalColors.Rose
                        else -> NernalColors.Amber
                    }
                    Surface(
                        color = if (state.authState == GoogleAuthState.NOT_SIGNED_IN) Color(0xFF282213) else NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.authState == GoogleAuthState.NOT_SIGNED_IN) NernalColors.Amber.copy(alpha = 0.65f) else NernalColors.Border,
                        ),
                        modifier = Modifier
                            .size(width = 46.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenAccount),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Google Account Profile",
                                tint = if (state.authState == GoogleAuthState.SIGNED_IN) NernalColors.Ink else NernalColors.InkMuted,
                                modifier = Modifier.size(22.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(5.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                            )
                        }
                    }

                    val currentContext = LocalContext.current
                    Surface(
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier
                            .size(width = 46.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { openDeviceWifiSettings(currentContext) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = "Device Wi-Fi Settings",
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }
            }

            // RIGHT (right-to-left from right edge): Settings, Night, Eye (if camera)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.capabilities.hasCamera) {
                    val eyeColor = when {
                        !state.cameraMotionWakeEnabled -> NernalColors.InkMuted
                        state.motionDetected -> NernalColors.Emerald
                        else -> NernalColors.Emerald.copy(alpha = 0.75f)
                    }
                    Surface(
                        color = if (state.motionDetected && state.cameraMotionWakeEnabled) Color(0xFF0E3A2C) else NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.cameraMotionWakeEnabled) eyeColor.copy(alpha = 0.65f) else NernalColors.Border,
                        ),
                        modifier = Modifier
                            .size(width = 46.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleCameraWake),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.cameraMotionWakeEnabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = "Camera Motion Wake",
                                tint = eyeColor,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }

                UtilityIconButton(
                    icon = Icons.Rounded.Bedtime,
                    contentDescription = "Night Mode",
                    onClick = onDimScreen,
                )

                // Settings: All the way to the right
                val hasFilter = (state.availableYears.isNotEmpty() && state.selectedPhotoYears.size < state.availableYears.size && state.selectedPhotoYears.isNotEmpty()) || state.photoAlbumFilter != null
                Surface(
                    color = if (hasFilter) NernalColors.CardSubtle else NernalColors.Card,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (hasFilter) NernalColors.Gold.copy(alpha = 0.65f) else NernalColors.Border,
                    ),
                    modifier = Modifier
                        .size(width = 46.dp, height = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSettings),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Frame Settings",
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // Photo Carousel Card
        NernalCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            containerColor = Color.Black,
            cornerRadius = 24.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(state.activePhotos.size) {
                            if (state.activePhotos.size <= 1) return@pointerInput
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragCancel = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag < -60f) onNextPhoto()
                                    else if (totalDrag > 60f) onPreviousPhoto()
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                },
                            )
                        }
                        .clickable(enabled = state.activePhotos.isNotEmpty(), onClick = onInspectPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    val hasLocalFilter = (state.availableYears.isNotEmpty() && state.selectedPhotoYears.size < state.availableYears.size && state.selectedPhotoYears.isNotEmpty()) || state.photoAlbumFilter != null
                    val needsSignIn = state.authState != GoogleAuthState.SIGNED_IN && state.customSharedAlbumUrls.isEmpty() && state.allPhotos.isEmpty()
                    val noAlbumsSelected = state.authState == GoogleAuthState.SIGNED_IN &&
                        state.customSharedAlbumUrls.isEmpty() &&
                        state.allPhotos.isEmpty()
                    PhotoStageContent(
                        photo = state.currentPhoto,
                        bitmap = state.currentBitmap,
                        isLoading = state.photoLoading || (state.albumsSyncing && state.allPhotos.isEmpty()),
                        favoritesOnly = state.favoritesOnly,
                        hasFilter = hasLocalFilter,
                        needsSignIn = needsSignIn,
                        noAlbumsSelected = noAlbumsSelected,
                        errorMessage = state.albumError,
                        onShowAll = onResetFilters,
                        onRetry = onRefreshAlbums,
                        onNextPhoto = onNextPhoto,
                        onOpenAccount = onOpenAccount,
                        onHideCarousel = onToggleCalendarPrimary?.let { fn -> { fn(true) } },
                    )
                    if (state.isCurrentStarred && state.currentPhoto != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, NernalColors.Amber.copy(alpha = 0.75f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Favorite photo",
                                    tint = NernalColors.Amber,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                PhotoControlBar(
                    photo = state.currentPhoto,
                    total = state.activePhotos.size,
                    isStarred = state.isCurrentStarred,
                    autoAdvance = state.autoAdvance,
                    favoritesOnly = state.favoritesOnly,
                    onPrev = onPreviousPhoto,
                    onNext = onNextPhoto,
                    onToggleAuto = onToggleAutoAdvance,
                    onToggleStar = onToggleStar,
                    onToggleFavoritesFilter = onToggleFavoritesFilter,
                    onEnterFullscreen = onEnterFullscreen,
                    onHide = onToggleCalendarPrimary?.let { fn -> { fn(true) } },
                )
            }
        }
    }
}

@Composable
private fun UtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isBusy: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = NernalColors.Card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NernalColors.Border),
        modifier = Modifier
            .size(width = 46.dp, height = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NernalColors.Accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = NernalColors.Ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyAlbumStateContent(
    isSignedIn: Boolean,
    errorMessage: String?,
    onOpenAccount: () -> Unit,
    onHideCarousel: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            tint = NernalColors.Gold,
            modifier = Modifier.size(44.dp),
        )

        Text(
            text = "No Photo Albums Added Yet",
            color = NernalColors.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Add photos to turn this side of the screen into a digital family picture frame, or hide this carousel to make your Calendar & Tasks full-screen.",
            color = NernalColors.InkMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.95f),
        )

        if (errorMessage != null) {
            Surface(
                color = NernalColors.Rose.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NernalColors.Rose.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorMessage,
                    color = NernalColors.Rose,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Two Instruction Guidance Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Card 1: How to Add Albums
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NernalColors.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = NernalColors.Gold.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("1", color = NernalColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "How to Add Photo Albums",
                            color = NernalColors.Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = "• Shared Link (Easiest): In Google Photos on your phone or computer, open any album, tap Share → 'Create Link', then tap below to paste the link.\n• Google Sign-In: Sign into your Google account to choose private albums or launch the Google Photo Picker.",
                        color = NernalColors.InkMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }

            // Card 2: How to Hide the Carousel
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NernalColors.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = NernalColors.Accent.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("2", color = NernalColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "How to Hide the Photo Carousel",
                            color = NernalColors.Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = "Don't want photos? Tap the collapse icon in the bottom-right bar, or tap the button below to expand your Calendar and Tasks to full screen.",
                        color = NernalColors.InkMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onOpenAccount,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = NernalColors.Bg,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSignedIn) "Choose / Add Albums" else "Sign In / Add Album",
                    color = NernalColors.Bg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (onHideCarousel != null) {
                Button(
                    onClick = onHideCarousel,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardSubtle),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewAgenda,
                        contentDescription = null,
                        tint = NernalColors.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hide Photo Carousel",
                        color = NernalColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoStageContent(
    photo: AlbumPhoto?,
    bitmap: Bitmap?,
    isLoading: Boolean,
    favoritesOnly: Boolean,
    hasFilter: Boolean = false,
    needsSignIn: Boolean = false,
    noAlbumsSelected: Boolean = false,
    errorMessage: String?,
    onShowAll: () -> Unit,
    onRetry: () -> Unit,
    onNextPhoto: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onHideCarousel: (() -> Unit)? = null,
) {
    when {
        (favoritesOnly || hasFilter) && photo == null -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.StarOutline,
                    contentDescription = null,
                    tint = NernalColors.Amber,
                    modifier = Modifier.size(42.dp),
                )
                Text(
                    text = if (favoritesOnly) "No favorite photos starred yet" else "No photos match this filter",
                    color = NernalColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (favoritesOnly) {
                        "Tap the ★ button on any photo to add it here."
                    } else {
                        "Try another year or album in Settings."
                    },
                    color = NernalColors.InkMuted,
                    fontSize = 14.sp,
                )
                Button(
                    onClick = onShowAll,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                ) {
                    Text("Show All Photos", color = NernalColors.Bg, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        photo == null && isLoading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = NernalColors.Gold)
                Text(
                    text = "Loading Google Photos...",
                    color = NernalColors.InkMuted,
                    fontSize = 15.sp,
                )
            }
        }

        photo == null -> {
            EmptyAlbumStateContent(
                isSignedIn = !needsSignIn,
                errorMessage = errorMessage,
                onOpenAccount = onOpenAccount,
                onHideCarousel = onHideCarousel,
                onRetry = onRetry,
            )
        }

        else -> {
            Crossfade(
                targetState = photo to bitmap,
                animationSpec = tween(durationMillis = 450),
                label = "PhotoCrossfade",
            ) { (targetPhoto, targetBitmap) ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (targetBitmap != null && !targetBitmap.isRecycled) {
                        val imgBitmap = remember(targetBitmap) { targetBitmap.asImageBitmap() }
                        Image(
                            bitmap = imgBitmap,
                            contentDescription = targetPhoto.albumTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(color = NernalColors.Gold)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BrokenImage,
                                contentDescription = null,
                                tint = NernalColors.InkMuted,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                text = "Unable to load photo",
                                color = NernalColors.InkMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                                ) {
                                    Text("Retry", color = NernalColors.Bg, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onNextPhoto,
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardSubtle),
                                ) {
                                    Text("Next Photo", color = NernalColors.Ink, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoControlBar(
    photo: AlbumPhoto?,
    total: Int,
    isStarred: Boolean,
    autoAdvance: Boolean,
    favoritesOnly: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleAuto: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onHide: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(NernalColors.Card)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: <, Pause/Play, >
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                onClick = onPrev,
                enabled = total > 1,
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous Photo",
                        tint = if (total > 1) NernalColors.Ink else NernalColors.InkMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Surface(
                onClick = onToggleAuto,
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (autoAdvance) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (autoAdvance) "Pause Slideshow" else "Play Slideshow",
                        tint = NernalColors.InkMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Surface(
                onClick = onNext,
                enabled = total > 1,
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next Photo",
                        tint = if (total > 1) NernalColors.Ink else NernalColors.InkMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        // Left-aligned right beside '>': Album name · year
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val titleLine = buildString {
                append(photo?.albumTitle ?: "Google Photos")
                photo?.year?.takeIf { it > 1970 }?.let { append(" · $it") }
            }
            Text(
                text = titleLine,
                color = NernalColors.Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Right: [★ Only / ☆ Off], Star, Fullscreen, Hide (uniform 48dp buttons with generous spacing)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                onClick = onToggleFavoritesFilter,
                color = if (favoritesOnly) Color(0xFF33260F) else NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (favoritesOnly) NernalColors.Amber else NernalColors.BorderSubtle,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = if (favoritesOnly) "Starred photos only" else "Starred filter off",
                        tint = if (favoritesOnly) NernalColors.Amber else NernalColors.InkMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (favoritesOnly) "Only" else "Off",
                        color = if (favoritesOnly) NernalColors.Amber else NernalColors.InkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Surface(
                onClick = onToggleStar,
                enabled = photo != null,
                color = if (isStarred) Color(0xFF33260F) else NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isStarred) NernalColors.Amber.copy(alpha = 0.75f) else NernalColors.BorderSubtle,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = if (isStarred) "Remove Favorite" else "Favorite Photo",
                        tint = if (isStarred) NernalColors.Amber else NernalColors.InkMuted,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            Surface(
                onClick = onEnterFullscreen,
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = "Fullscreen Slideshow",
                        tint = NernalColors.Ink,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            if (onHide != null) {
                Surface(
                    onClick = onHide,
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LastPage,
                            contentDescription = "Hide Photos",
                            tint = NernalColors.InkMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenSlideshowOverlay(
    state: NernalUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleStar: () -> Unit,
    onShowAll: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onExit: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    fun dismissKeyboardAndReturn() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = (context as? ComponentActivity)?.window?.decorView
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        onExit()
    }

    BackHandler {
        dismissKeyboardAndReturn()
    }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = (context as? ComponentActivity)?.window?.decorView
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        delay(30 * 60_000L)
        dismissKeyboardAndReturn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.activePhotos.size) {
                    if (state.activePhotos.size <= 1) return@pointerInput
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag < -60f) onNext()
                            else if (totalDrag > 60f) onPrev()
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                    )
                }
                .clickable { dismissKeyboardAndReturn() },
            contentAlignment = Alignment.Center,
        ) {
            PhotoStageContent(
                photo = state.currentPhoto,
                bitmap = state.currentBitmap,
                isLoading = state.photoLoading,
                favoritesOnly = state.favoritesOnly,
                errorMessage = state.albumError,
                onShowAll = onShowAll,
                onRetry = onRetry,
                onNextPhoto = onNext,
                onOpenAccount = onOpenAccount,
            )
            if (state.isCurrentStarred && state.currentPhoto != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, NernalColors.Amber.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Favorite photo",
                            tint = NernalColors.Amber,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.88f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NernalColors.Border.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${state.clockTime} ${state.clockAmPm}",
                    color = NernalColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (state.favoritesOnly) {
                    Surface(
                        color = NernalColors.Amber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, NernalColors.Amber.copy(alpha = 0.8f)),
                    ) {
                        Text(
                            text = "★ Favs (${state.activePhotos.size})",
                            color = NernalColors.Amber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                state.currentPhoto?.let { p ->
                    val label = buildString {
                        append(p.albumTitle)
                        p.year?.takeIf { it > 1970 }?.let { append(" · $it") }
                    }
                    Text(
                        text = label,
                        color = NernalColors.WarmAmber,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.88f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border.copy(alpha = 0.7f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(onClick = onPrev, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous", tint = NernalColors.Ink, modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = onToggleStar, modifier = Modifier.size(54.dp)) {
                    Icon(
                        imageVector = if (state.isCurrentStarred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (state.isCurrentStarred) NernalColors.Amber else NernalColors.InkMuted,
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Rounded.ChevronRight, "Next", tint = NernalColors.Ink, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { dismissKeyboardAndReturn() },
                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp),
                ) {
                    Icon(Icons.Rounded.FullscreenExit, null, tint = NernalColors.Ink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit", color = NernalColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DimOverlay(
    time: String,
    amPm: String,
    weather: WeatherData?,
    nextEvent: ReminderItem? = null,
    todayKey: String? = null,
    cameraWakeActive: Boolean = false,
    isManualNightMode: Boolean = false,
    alarmConfig: AlarmConfig = AlarmConfig(),
    onWake: () -> Unit,
) {
    // Pure opaque solid black night veil so background dashboard cards do not bleed through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onWake),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xDD0E1218),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, NernalColors.Border.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "$time $amPm",
                    color = NernalColors.Ink.copy(alpha = 0.92f),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )

                weather?.let {
                    Text(
                        text = "${it.currentTempF}°F · ${it.description}",
                        color = NernalColors.WarmAmber.copy(alpha = 0.85f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (nextEvent != null) {
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Event,
                                contentDescription = null,
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "NEXT: ${nextEvent.time ?: "All Day"}",
                                        color = NernalColors.Accent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val rel = when (nextEvent.dateKey) {
                                        todayKey -> "Today"
                                        else -> nextEvent.dateKey ?: ""
                                    }
                                    if (rel.isNotBlank()) {
                                        Text(
                                            text = "· $rel",
                                            color = NernalColors.WarmAmber,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                Text(
                                    text = nextEvent.text,
                                    color = NernalColors.Ink,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No upcoming events scheduled",
                        color = NernalColors.InkMuted.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isManualNightMode -> "Night mode is on · Tap anywhere to turn off"
                        cameraWakeActive -> "Tap anywhere or walk up to wake ${BuildConfig.APP_DISPLAY_NAME}"
                        else -> "Tap anywhere to wake ${BuildConfig.APP_DISPLAY_NAME}"
                    },
                    color = NernalColors.InkMuted.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                )

                if (alarmConfig.enabled) {
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Alarm set for ${alarmConfig.formattedTime}",
                                color = NernalColors.Gold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    state: NernalUiState,
    onSlideIntervalChange: (Int) -> Unit,
    onTogglePhotoYear: (Int) -> Unit = {},
    onToggleAllPhotoYears: () -> Unit = {},
    onAlbumFilterChange: (String?) -> Unit,
    onDimBrightnessChange: (Float) -> Unit,
    onGlobalBrightnessChange: (Float) -> Unit,
    onWakeOnEventReminderChange: (Boolean) -> Unit,
    onCameraMotionWakeChange: (Boolean) -> Unit = {},
    onAutoSleepMinutesChange: (Int) -> Unit = {},
    onAudibleEventAlertsChange: (Boolean) -> Unit = {},
    onSaveGeminiApiKey: (String) -> Unit = {},
    onStartGeminiKeyLan: () -> Unit = {},
    onStopGeminiKeyLan: () -> Unit = {},
    onScanGeminiKeyTasks: () -> Unit = {},
    onImportGeminiKeyCandidate: (GeminiKeyCandidate, Boolean) -> Unit = { _, _ -> },
    onTestAudio: () -> Unit = {},
    onUpdateAlarmConfig: (AlarmConfig) -> Unit = {},
    onTestAlarm: () -> Unit = {},
    onPiSyncEnabledChange: (Boolean) -> Unit = {},
    onOpenPiSync: () -> Unit = {},
    onCalendarViewModeChange: (CalendarViewMode) -> Unit = {},
    onCalendarPrimaryModeChange: (Boolean) -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
    onRefreshAlbums: () -> Unit = {},
    onRemoveCustomSharedUrl: (String) -> Unit,
    onOpenGoogleAccount: () -> Unit,
    onOpenHelp: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .fillMaxHeight(0.88f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Settings, null, tint = NernalColors.Gold)
                    Text(
                        text = "Frame Display Settings",
                        color = NernalColors.Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                val settingsScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(settingsScrollState)
                        .verticalScrollbar(settingsScrollState)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                // Shortcut to Google Account, Calendars, Tasks & Location
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenGoogleAccount),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Google Account, Calendars & Location",
                                    color = NernalColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Choose calendars, task lists, and home location",
                                    color = NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        Text(
                            text = "Configure →",
                            color = NernalColors.Gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Shortcut to Device Wi-Fi & Optional Default Home App
                val settingsContext = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenWifiSettings),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(22.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Device Wi-Fi Settings",
                                    color = NernalColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Manage Wi-Fi networks",
                                    color = NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    if (BuildConfig.IS_NERNAL_FAMILY_BUILD) {
                        Surface(
                            color = NernalColors.CardInner,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NernalColors.Border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    openDeviceHomeSettings(settingsContext)
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = NernalColors.Gold,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Default Home App",
                                        color = NernalColors.Ink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Kiosk launcher picker",
                                        color = NernalColors.InkMuted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Smart Hardware, Voice & Gemini AI (Auto-detected per device)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Detected Hardware & Smart Features",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HardwareBadgeChip("Camera", state.capabilities.hasCamera)
                        HardwareBadgeChip("Microphone", state.capabilities.hasMicrophone)
                        HardwareBadgeChip("Speakers", state.capabilities.hasAudioOutput)
                        HardwareBadgeChip("On-Device Speech", state.capabilities.hasOnDeviceSpeech)
                    }

                    if (state.capabilities.hasCamera) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Camera Motion Wake",
                                    color = NernalColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = state.cameraError ?: "Automatically wakes the dimmed screen when movement is detected",
                                    color = if (state.cameraError != null) NernalColors.Amber else NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                )
                            }
                            Switch(
                                checked = state.cameraMotionWakeEnabled,
                                onCheckedChange = onCameraMotionWakeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NernalColors.Bg,
                                    checkedTrackColor = NernalColors.Emerald,
                                    uncheckedThumbColor = NernalColors.InkMuted,
                                    uncheckedTrackColor = NernalColors.CardSubtle,
                                ),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Auto-Sleep After Inactivity",
                                color = NernalColors.Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (mins in listOf(0, 5, 10, 15, 30)) {
                                    SettingChip(
                                        label = if (mins == 0) "Off" else "${mins}m",
                                        selected = state.autoSleepMinutes == mins,
                                        onClick = { onAutoSleepMinutesChange(mins) },
                                    )
                                }
                            }
                        }
                    }

                    if (state.capabilities.hasAudioOutput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Audible Event Alerts & Chime",
                                    color = NernalColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Plays a warm chime and announces upcoming events aloud",
                                    color = NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                )
                            }
                            Switch(
                                checked = state.audibleEventAlertsEnabled,
                                onCheckedChange = onAudibleEventAlertsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NernalColors.Bg,
                                    checkedTrackColor = NernalColors.Gold,
                                    uncheckedThumbColor = NernalColors.InkMuted,
                                    uncheckedTrackColor = NernalColors.CardSubtle,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = onTestAudio,
                            border = BorderStroke(1.dp, NernalColors.Border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Test Chime & Voice", color = NernalColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (state.capabilities.hasVoiceInputAndOutput || state.capabilities.hasAudioOutput) {
                        var draftGeminiKey by remember(state.geminiApiKey) { mutableStateOf(state.geminiApiKey) }
                        var showKey by remember { mutableStateOf(false) }

                        Surface(
                            color = NernalColors.CardInner,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = NernalColors.Gold,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "Gemini API Key (Optional — Unlocks \"Super ${BuildConfig.APP_DISPLAY_NAME}\")",
                                        color = NernalColors.Gold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    text = "On-device speech already handles time, weather, schedule, tasks, and screen controls. Add your Gemini API key for conversational AI, camera vision (\"What am I holding?\"), and Memory Book recipes.",
                                    color = NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                                OutlinedTextField(
                                    value = draftGeminiKey,
                                    onValueChange = { draftGeminiKey = it },
                                    label = { Text("Gemini API Key (AIza...)") },
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(
                                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (showKey) "Hide key" else "Show key",
                                                tint = NernalColors.InkMuted,
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = NernalColors.Bg,
                                        unfocusedContainerColor = NernalColors.Bg,
                                        focusedBorderColor = NernalColors.Gold,
                                        unfocusedBorderColor = NernalColors.Border,
                                        focusedTextColor = NernalColors.Ink,
                                        unfocusedTextColor = NernalColors.Ink,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { onSaveGeminiApiKey(draftGeminiKey) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(44.dp),
                                    ) {
                                        Text("Save Key", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (state.geminiKeyLanUrl == null) onStartGeminiKeyLan() else onStopGeminiKeyLan()
                                        },
                                        border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(44.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Wifi,
                                            contentDescription = null,
                                            tint = NernalColors.Accent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (state.geminiKeyLanUrl == null) "Send from Phone/PC" else "Stop Wi-Fi Receiver",
                                            color = NernalColors.Accent,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    if (state.authState == GoogleAuthState.SIGNED_IN) {
                                        OutlinedButton(
                                            onClick = onScanGeminiKeyTasks,
                                            border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(44.dp),
                                        ) {
                                            Text("Pick from Google Tasks", color = NernalColors.Gold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                    if (state.geminiApiKey.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                draftGeminiKey = ""
                                                onSaveGeminiApiKey("")
                                            },
                                            border = BorderStroke(1.dp, NernalColors.Border),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(44.dp),
                                        ) {
                                            Text("Clear Key", color = NernalColors.InkMuted, fontSize = 13.sp)
                                        }
                                    }
                                }

                                state.geminiKeyLanUrl?.let { lanUrl ->
                                    Surface(
                                        color = NernalColors.Bg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            QrCodeImage(content = lanUrl, size = 112.dp)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    text = "Scan QR or open on your phone/laptop (same Wi-Fi):",
                                                    color = NernalColors.InkMuted,
                                                    fontSize = 12.sp,
                                                )
                                                Text(
                                                    text = lanUrl,
                                                    color = NernalColors.Accent,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }

                                state.geminiKeyScanNotice?.let { notice ->
                                    Text(
                                        text = notice,
                                        color = NernalColors.WarmAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                if (state.geminiKeyCandidates.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (candidate in state.geminiKeyCandidates) {
                                            Surface(
                                                color = NernalColors.Bg,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.55f)),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = candidate.taskTitle,
                                                            color = NernalColors.Ink,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                        Text(
                                                            text = "${candidate.maskedKey} · removes task after import",
                                                            color = NernalColors.InkMuted,
                                                            fontSize = 11.sp,
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { onImportGeminiKeyCandidate(candidate, true) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.height(40.dp),
                                                    ) {
                                                        Text("Use Key", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Morning Alarm & Daily Brief
                MorningAlarmSettingsSection(
                    alarmConfig = state.alarmConfig,
                    onUpdateAlarmConfig = onUpdateAlarmConfig,
                    onTestAlarm = onTestAlarm,
                )

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Optional Nernal Raspberry Pi Home Hub Sync
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Nernal Raspberry Pi Sync (Optional)",
                                color = NernalColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Sync reminders, Memory Book, and ★ favorites with a Nernal Pi server on your Wi-Fi",
                                color = NernalColors.InkMuted,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = state.piSyncEnabled,
                            onCheckedChange = onPiSyncEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NernalColors.Bg,
                                checkedTrackColor = NernalColors.Emerald,
                                uncheckedThumbColor = NernalColors.InkMuted,
                                uncheckedTrackColor = NernalColors.CardSubtle,
                            ),
                        )
                    }
                    if (state.piSyncEnabled) {
                        OutlinedButton(
                            onClick = onOpenPiSync,
                            border = BorderStroke(1.dp, NernalColors.Emerald.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = NernalColors.Emerald,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Configure Pi Sync (${state.piBaseUrl})", color = NernalColors.Emerald, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Layout: Make Calendar Primary (Hide Photos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Make Calendar Primary (Hide Photos)",
                            color = NernalColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Removes photo slideshow to make Calendar view primary, with a dedicated tab for tasks",
                            color = NernalColors.InkMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = state.calendarPrimaryMode,
                        onCheckedChange = onCalendarPrimaryModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NernalColors.Bg,
                            checkedTrackColor = NernalColors.Gold,
                            uncheckedThumbColor = NernalColors.InkMuted,
                            uncheckedTrackColor = NernalColors.CardSubtle,
                        ),
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Calendar View Mode (Itinerary, Month, Week)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Calendar View Mode",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (mode in CalendarViewMode.entries) {
                            SettingChip(
                                label = mode.label,
                                selected = state.calendarViewMode == mode,
                                onClick = { onCalendarViewModeChange(mode) },
                            )
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Wake Screen on Event Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Wake Screen for Upcoming Events",
                            color = NernalColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Wakes display and alerts when an event or reminder time arrives within 5 minutes",
                            color = NernalColors.InkMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = state.wakeOnEventReminder,
                        onCheckedChange = onWakeOnEventReminderChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NernalColors.Bg,
                            checkedTrackColor = NernalColors.Gold,
                            uncheckedThumbColor = NernalColors.InkMuted,
                            uncheckedTrackColor = NernalColors.CardSubtle,
                        ),
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Manage Imported Photo Albums
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Manage Imported Photo Albums",
                            color = NernalColors.Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(
                            onClick = onRefreshAlbums,
                            enabled = !state.albumsSyncing,
                            border = BorderStroke(1.dp, NernalColors.Border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (state.albumsSyncing) "Syncing..." else "Refresh Photos",
                                color = NernalColors.Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "How to Import Google Photos by Link:",
                                color = NernalColors.Gold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "1. Open Google Photos on your phone or computer.\n" +
                                    "2. Open any album, tap Share and copy the link (e.g. photos.app.goo.gl/...).\n" +
                                    "3. Add a Google Task with the album link in the title or notes (e.g. 'Photos: https://photos.app.goo.gl/...').\n" +
                                    "4. ${BuildConfig.APP_DISPLAY_NAME} will auto-detect the link, import all photos into your slideshow rotation, and automatically complete the task!",
                                color = NernalColors.Ink,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )
                        }
                    }

                    if (state.customSharedAlbumUrls.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.customSharedAlbumUrls.forEachIndexed { idx, url ->
                                val resolvedTitle = state.allPhotos.firstOrNull { it.albumUrl == url }?.albumTitle
                                    ?.takeIf { it.isNotBlank() && !it.equals("Google Photos", ignoreCase = true) }
                                    ?: "Imported Album #${idx + 1}"
                                val photoCount = state.allPhotos.count { it.albumUrl == url }
                                Surface(
                                    color = NernalColors.CardInner,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = resolvedTitle,
                                                    color = NernalColors.Ink,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                if (photoCount > 0) {
                                                    Surface(
                                                        color = NernalColors.CardSubtle,
                                                        shape = RoundedCornerShape(6.dp),
                                                    ) {
                                                        Text(
                                                            text = "$photoCount photos",
                                                            color = NernalColors.Gold,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = url,
                                                color = NernalColors.InkMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveCustomSharedUrl(url) },
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove Album",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                Text(
                    text = "These display settings stay on this frame only and do not sync between devices.",
                    color = NernalColors.InkMuted,
                    fontSize = 12.sp,
                )

                // Photo Display Time (local per device)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Photo Display Time",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (sec in listOf(5, 10, 15, 20, 30, 60)) {
                            SettingChip(
                                label = "${sec}s",
                                selected = state.slideIntervalSec == sec,
                                onClick = { onSlideIntervalChange(sec) },
                            )
                        }
                    }
                }

                // Photo Year Filter (multi-select)
                if (state.availableYears.isNotEmpty()) {
                    val allLit = state.isAllYearsSelected || state.isAllYearsMode || (state.availableYears.isNotEmpty() && state.selectedPhotoYears.size == state.availableYears.size)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Photo Year Filter",
                                color = NernalColors.Gold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = when {
                                    allLit -> "All Years"
                                    state.selectedPhotoYears.isEmpty() -> "None selected (showing all)"
                                    else -> "${state.selectedPhotoYears.size} of ${state.availableYears.size} selected"
                                },
                                color = NernalColors.InkMuted,
                                fontSize = 12.sp,
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SettingChip(
                                label = "All Years",
                                selected = allLit,
                                onClick = onToggleAllPhotoYears,
                            )
                            for (yr in state.availableYears) {
                                val isSelected = allLit || yr in state.selectedPhotoYears
                                SettingChip(
                                    label = yr.toString(),
                                    selected = isSelected,
                                    onClick = { onTogglePhotoYear(yr) },
                                )
                            }
                        }
                    }
                }

                // Shared Album Filter (local per device)
                if (state.availableAlbums.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Album Filter",
                            color = NernalColors.Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SettingChip(
                                label = "All Albums",
                                selected = state.photoAlbumFilter == null,
                                onClick = { onAlbumFilterChange(null) },
                            )
                            for (album in state.availableAlbums) {
                                SettingChip(
                                    label = album,
                                    selected = state.photoAlbumFilter == album,
                                    onClick = { onAlbumFilterChange(album) },
                                )
                            }
                        }
                    }
                }

                // Tablet Global Screen Brightness (local per device)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tablet Screen Brightness",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val levels = listOf(
                            "Dim (30%)" to 0.30f,
                            "Balanced (60%)" to 0.60f,
                            "Bright (85%)" to 0.85f,
                            "Max (100%)" to 1.00f,
                        )
                        for ((label, level) in levels) {
                            val isSelected = kotlin.math.abs(state.globalBrightness - level) < 0.06f
                            SettingChip(
                                label = label,
                                selected = isSelected,
                                onClick = { onGlobalBrightnessChange(level) },
                            )
                        }
                    }
                }

                // Night Dim Brightness (local per device)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sleep / Dim Brightness",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val levels = listOf("Dimmer" to 0.10f, "Balanced" to 0.16f, "Brighter" to 0.26f)
                        for ((label, level) in levels) {
                            val isSelected = kotlin.math.abs(state.dimBrightness - level) < 0.03f
                            SettingChip(
                                label = label,
                                selected = isSelected,
                                onClick = { onDimBrightnessChange(level) },
                            )
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                Button(
                    onClick = onOpenHelp,
                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardInner),
                    border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = NernalColors.Gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Open Built-In User Guide",
                        color = NernalColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // App Updates
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "App Updates",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Current Version",
                                        color = NernalColors.InkMuted,
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        text = "v${com.nernal.frame.BuildConfig.VERSION_NAME}",
                                        color = NernalColors.Ink,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                Button(
                                    onClick = onCheckForUpdates,
                                    enabled = !state.isCheckingForUpdate && !state.isDownloadingUpdate,
                                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardSubtle),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp),
                                ) {
                                    if (state.isCheckingForUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = NernalColors.Gold,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text("Check for Updates", color = NernalColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (!state.updateNotice.isNullOrBlank()) {
                                Text(
                                    text = state.updateNotice,
                                    color = NernalColors.Emerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            if (!state.updateError.isNullOrBlank()) {
                                Text(
                                    text = state.updateError,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                )
                            }
                            if (state.updateRelease != null) {
                                Surface(
                                    color = Color(0xFF33260F),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NernalColors.Amber),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "New Release: ${state.updateRelease.tagName}",
                                                color = NernalColors.Amber,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (state.updateRelease.title.isNotBlank()) {
                                                Text(
                                                    text = state.updateRelease.title,
                                                    color = NernalColors.InkMuted,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = onInstallUpdate,
                                            enabled = !state.isDownloadingUpdate,
                                            colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Amber),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(40.dp),
                                        ) {
                                            if (state.isDownloadingUpdate) {
                                                val pct = (state.updateDownloadProgress * 100).toInt()
                                                Text("$pct%", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Update Now", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = NernalColors.BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hasActiveFilter = !state.isAllYearsSelected || state.photoAlbumFilter != null || state.favoritesOnly
                if (hasActiveFilter) {
                    TextButton(onClick = onResetFilters, modifier = Modifier.height(48.dp)) {
                        Text("Reset Filters", color = NernalColors.InkMuted, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Done", color = NernalColors.Bg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}

@Composable
fun UserGuideDialog(
    onOpenFeedback: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.88f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = "${BuildConfig.APP_DISPLAY_NAME} User Guide",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NernalColors.Ink,
                        )
                    }

                    Button(
                        onClick = onOpenFeedback,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardInner),
                        border = BorderStroke(1.dp, NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Send Feedback", color = NernalColors.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                val guideScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(guideScrollState)
                        .verticalScrollbar(guideScrollState)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserGuideSection(
                        title = "1. Overview & Device-Adaptive Features",
                        content = "${BuildConfig.APP_DISPLAY_NAME} automatically detects your device's hardware and unlocks matching capabilities:\n" +
                            "• Left Wing: Live clock, local weather, NWS severe weather alerts, Doppler radar, and Home Hub (calendar & tasks).\n" +
                            "• Right Wing: High-resolution photo slideshow, favorite memories, and adaptive controls (Speak, Camera Wake, Memory Book, Pi Sync)."
                    )

                    UserGuideSection(
                        title = "2. Clock, Weather Assessment & Doppler Radar",
                        content = "• The top card displays a large high-contrast clock and today's day/date with location underneath, current temperature, today's high/low, and upcoming forecast cards. Tap the chevron icon to collapse or expand weather.\n" +
                            "• Tap the current weather area or any future forecast card to open a detailed Weather Assessment with plain-English conditions, hourly outlook, wind & gusts, UV index, and sunrise/sunset.\n" +
                            "• Active NWS warnings appear as a red warning icon by the date; tap to view details and dismiss individual alerts.\n" +
                            "• Tap the Radar icon right beside the current temperature to open animated pan/zoom Doppler radar."
                    )

                    UserGuideSection(
                        title = "3. Home Hub: Calendar & Tasks",
                        content = "• All Tab: Chronological list of upcoming events and active tasks.\n" +
                            "• Events Tab: Tap 'Events' to switch between Itinerary, 3-Day, Week, and Month views. In Month view, tap any day to pop out its events and tasks, tap '+ Add Task / Event' to add either on that date, or tap an event for full details.\n" +
                            "• Tasks Tab: Focused view for your Google Task lists with checkboxes to mark items done.\n" +
                            "• Add (+) Button: Tap the '+' button in the top right to quickly add a Task (choosing which Google Task list or setting a Default) or an Event.\n" +
                            "• Filter Dropdown: Filter individual task lists per tab, and use the 5-second Undo bar if you delete an item by mistake."
                    )

                    UserGuideSection(
                        title = "4. Quick-Add (+) Button & Default Task List",
                        content = "• Tap the '+' button in the top right (or inside a Month day pop-out) to open the New Entry dialog.\n" +
                            "• Choose Task or Event. When adding a Task, pick which Google Task list to add to (pre-selected to your Default list, with a quick 'Set as default' toggle or in Settings → Google Account → Task Lists).\n" +
                            "• Events sync directly to your primary Google Calendar and Tasks sync directly to your chosen Google Task list."
                    )

                    UserGuideSection(
                        title = "5. Photo Slideshow, Swiping & Favorites",
                        content = "• Swipe left or right on the photo stage (normal or fullscreen) to browse, or tap a photo to inspect fullscreen for 10 seconds.\n" +
                            "• At the bottom of the photo card: browse with < / ⏸ / > (album name appears right beside >), tap [★ Only / ☆ Off] to filter starred photos, tap ★ to favorite, tap Fullscreen for 30-minute viewing, or tap the bottom-right collapse icon (▸|) to expand Calendar full-screen (tap the same bottom-right restore icon |◂ to bring photos back)."
                    )

                    UserGuideSection(
                        title = "6. Google Photos: Picker & Shared Links",
                        content = "• Google Photos Picker: In Settings → Google Account → Photos, launch Picker to securely select specific photos straight from your library (cached locally on-frame).\n" +
                            "• Shared Links: Add a photos.app.goo.gl album link directly in Google Account, OR include the link in a Google Task title/notes and ${BuildConfig.APP_DISPLAY_NAME} will import it automatically."
                    )

                    UserGuideSection(
                        title = "7. Night Mode, Camera Motion Wake & Event Alerts",
                        content = "• Tap the Moon icon (Night Mode) in the top right for pure black ambient night mode — Night Mode stays on until you tap the screen to turn it off.\n" +
                            "• On devices with a front camera, Auto-Sleep dims after inactivity and the Eye icon wakes the screen hands-free when someone walks up.\n" +
                            "• 'Wake on Event Reminder' alerts 1–5 minutes before a scheduled event and plays a chime + spoken announcement on devices with speakers."
                    )

                    UserGuideSection(
                        title = "8. Voice Assistant & Super ${BuildConfig.APP_DISPLAY_NAME} (Gemini AI)",
                        content = "• On devices with a microphone and speakers, tap the outlined 'Speak' button to ask for time, weather, morning briefing, schedule/tasks, or control photos and radar using on-device speech — no API key needed.\n" +
                            "• Add an optional Gemini API key in Settings (type it, send from your phone/laptop over Wi-Fi, or pick a key from Google Tasks) to unlock conversational AI, camera vision ('What am I holding?', 'Read this'), and the voice-managed Memory Book."
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onOpenFeedback,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.CardInner),
                        border = BorderStroke(1.dp, NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Send Feedback", color = NernalColors.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Got It", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    state: NernalUiState,
    onSubmit: (comment: String, includeLogs: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }
    val senderLabel = state.accountProfile?.email?.takeIf { it.isNotBlank() }
        ?: "Anonymous (not signed in)"

    Dialog(
        onDismissRequest = { if (!state.isSubmittingFeedback) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.72f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = NernalColors.Gold,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Send Private Feedback",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NernalColors.Ink,
                    )
                }

                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "From: $senderLabel",
                            color = NernalColors.Gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Reports are sent privately to the developer to fix bugs and prioritize requested features — never posted publicly.",
                            color = NernalColors.InkMuted,
                            fontSize = 12.sp,
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Bug report or feature idea", color = NernalColors.InkMuted) },
                    placeholder = {
                        Text(
                            text = "Tell us what happened or what feature would help...",
                            color = NernalColors.InkMuted.copy(alpha = 0.7f),
                        )
                    },
                    minLines = 4,
                    maxLines = 7,
                    enabled = !state.isSubmittingFeedback,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NernalColors.Bg,
                        unfocusedContainerColor = NernalColors.Bg,
                        focusedBorderColor = NernalColors.Gold,
                        unfocusedBorderColor = NernalColors.Border,
                        focusedTextColor = NernalColors.Ink,
                        unfocusedTextColor = NernalColors.Ink,
                        cursorColor = NernalColors.Gold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.isSubmittingFeedback) { includeLogs = !includeLogs },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = includeLogs,
                            onCheckedChange = { if (!state.isSubmittingFeedback) includeLogs = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NernalColors.Gold,
                                checkmarkColor = NernalColors.Bg,
                                uncheckedColor = NernalColors.InkMuted,
                            ),
                        )
                        Text(
                            text = "Include app diagnostic logs (recommended for bug reports)",
                            color = NernalColors.Ink,
                            fontSize = 13.sp,
                        )
                    }
                }

                if (!state.feedbackError.isNullOrBlank()) {
                    Text(
                        text = state.feedbackError,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (!state.feedbackNotice.isNullOrBlank()) {
                    Text(
                        text = state.feedbackNotice,
                        color = NernalColors.Emerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !state.isSubmittingFeedback,
                        border = BorderStroke(1.dp, NernalColors.Border),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = if (state.feedbackNotice != null) "Close" else "Cancel",
                            color = NernalColors.InkMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = { onSubmit(comment, includeLogs) },
                        enabled = comment.isNotBlank() && !state.isSubmittingFeedback && state.feedbackNotice == null,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        if (state.isSubmittingFeedback) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NernalColors.Bg,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sending...", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                                tint = NernalColors.Bg,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Send Feedback", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserGuideSection(title: String, content: String) {
    Surface(
        color = NernalColors.CardInner,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = NernalColors.Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = content,
                color = NernalColors.Ink,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun HardwareBadgeChip(label: String, available: Boolean) {
    Surface(
        color = if (available) NernalColors.Emerald.copy(alpha = 0.14f) else NernalColors.CardInner,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (available) NernalColors.Emerald.copy(alpha = 0.6f) else NernalColors.BorderSubtle,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (available) NernalColors.Emerald else NernalColors.InkMuted.copy(alpha = 0.4f)),
            )
            Text(
                text = label,
                color = if (available) NernalColors.Ink else NernalColors.InkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MorningAlarmSettingsSection(
    alarmConfig: AlarmConfig,
    onUpdateAlarmConfig: (AlarmConfig) -> Unit,
    onTestAlarm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = NernalColors.Gold,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Morning Alarm & Daily Brief",
                        color = NernalColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Wakes Night Mode at ${alarmConfig.formattedTime} and reads your weather & day",
                        color = NernalColors.InkMuted,
                        fontSize = 12.sp,
                    )
                }
            }
            Switch(
                checked = alarmConfig.enabled,
                onCheckedChange = { onUpdateAlarmConfig(alarmConfig.copy(enabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NernalColors.Bg,
                    checkedTrackColor = NernalColors.Gold,
                    uncheckedThumbColor = NernalColors.InkMuted,
                    uncheckedTrackColor = NernalColors.CardSubtle,
                ),
            )
        }

        // Time steppers: Hour (- / +), Minute (-5 / +5), AM/PM toggle, Test button
        val isPm = alarmConfig.hour24 >= 12
        val hour12 = when {
            alarmConfig.hour24 == 0 -> 12
            alarmConfig.hour24 > 12 -> alarmConfig.hour24 - 12
            else -> alarmConfig.hour24
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingStepperPill(
                label = "Hr",
                valueText = hour12.toString(),
                onDecrement = {
                    val prev = (alarmConfig.hour24 + 23) % 24
                    onUpdateAlarmConfig(alarmConfig.copy(hour24 = prev))
                },
                onIncrement = {
                    val next = (alarmConfig.hour24 + 1) % 24
                    onUpdateAlarmConfig(alarmConfig.copy(hour24 = next))
                },
            )
            SettingStepperPill(
                label = "Min",
                valueText = alarmConfig.minute.toString().padStart(2, '0'),
                onDecrement = {
                    val prev = (alarmConfig.minute + 55) % 60
                    onUpdateAlarmConfig(alarmConfig.copy(minute = prev))
                },
                onIncrement = {
                    val next = (alarmConfig.minute + 5) % 60
                    onUpdateAlarmConfig(alarmConfig.copy(minute = next))
                },
            )
            SettingChip(
                label = if (isPm) "PM" else "AM",
                selected = true,
                onClick = {
                    val toggled = if (isPm) alarmConfig.hour24 - 12 else alarmConfig.hour24 + 12
                    onUpdateAlarmConfig(alarmConfig.copy(hour24 = toggled))
                },
            )
            OutlinedButton(
                onClick = onTestAlarm,
                border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Test Alarm", color = NernalColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Day-of-week chips (1=Mon .. 7=Sun)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val days = listOf(7 to "Su", 1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa")
            for ((dayNum, dayLabel) in days) {
                val active = dayNum in alarmConfig.daysOfWeek
                SettingChip(
                    label = dayLabel,
                    selected = active,
                    onClick = {
                        val next = if (active) alarmConfig.daysOfWeek - dayNum else alarmConfig.daysOfWeek + dayNum
                        if (next.isNotEmpty()) {
                            onUpdateAlarmConfig(alarmConfig.copy(daysOfWeek = next))
                        }
                    },
                )
            }
        }

        // Daily Brief timing
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Morning Daily Brief",
                color = NernalColors.InkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (timing in AlarmBriefTiming.entries) {
                    SettingChip(
                        label = timing.label,
                        selected = alarmConfig.speakBriefTiming == timing,
                        onClick = { onUpdateAlarmConfig(alarmConfig.copy(speakBriefTiming = timing)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingStepperPill(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Surface(
        color = NernalColors.CardInner,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NernalColors.Border),
        modifier = Modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(38.dp)) {
                Text("-", color = NernalColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = NernalColors.InkMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Text(valueText, color = NernalColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onIncrement, modifier = Modifier.size(38.dp)) {
                Text("+", color = NernalColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) NernalColors.Gold else NernalColors.CardInner,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) NernalColors.Gold else NernalColors.Border),
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Text(
                text = label,
                color = if (selected) NernalColors.Bg else NernalColors.Ink,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
