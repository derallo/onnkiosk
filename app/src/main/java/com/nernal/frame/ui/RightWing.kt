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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.AlbumPhoto
import com.nernal.frame.data.CalendarViewMode
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.WeatherData
import kotlinx.coroutines.delay

fun openDeviceWifiSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
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
    onOpenHelp: () -> Unit = {},
    onOpenAccount: () -> Unit,
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
        // Top Utility Bar (zero camera/mic/speaker buttons per frame hardware)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // LEFT: Account, Wi-Fi, Help
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tappable Google Account / Sources indicator: profile silhouette with status dot
                val dotColor = when {
                    state.authState == GoogleAuthState.SIGNED_IN && state.googleSyncing ->
                        NernalColors.WarmAmber
                    state.authState == GoogleAuthState.SIGNED_IN ->
                        NernalColors.Emerald
                    state.authState == GoogleAuthState.SIGNING_IN ->
                        NernalColors.Accent
                    state.authState == GoogleAuthState.ERROR ->
                        NernalColors.Rose
                    else ->
                        NernalColors.Amber
                }
                Surface(
                    color = if (state.authState == GoogleAuthState.NOT_SIGNED_IN) Color(0xFF282213) else NernalColors.Card,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (state.authState == GoogleAuthState.NOT_SIGNED_IN) NernalColors.Amber.copy(alpha = 0.65f) else NernalColors.Border,
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAccount),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Google Account Profile",
                            tint = if (state.authState == GoogleAuthState.SIGNED_IN) NernalColors.Ink else NernalColors.InkMuted,
                            modifier = Modifier.size(24.dp),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openDeviceWifiSettings(currentContext) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = "Device Wi-Fi Settings",
                            tint = NernalColors.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Surface(
                    color = NernalColors.Card,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenHelp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "?",
                            color = NernalColors.Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // RIGHT: Refresh, Dim, Fullscreen, SETTINGS (far right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UtilityIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Reshuffle / Sync Photos",
                    isBusy = state.albumsSyncing,
                    onClick = onRefreshAlbums,
                )
                UtilityIconButton(
                    icon = Icons.Rounded.Bedtime,
                    contentDescription = "Dim Screen",
                    onClick = onDimScreen,
                )
                UtilityIconButton(
                    icon = Icons.Rounded.Fullscreen,
                    contentDescription = "Fullscreen Slideshow",
                    onClick = onEnterFullscreen,
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
                        .size(48.dp)
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
            .size(48.dp)
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
                        text = "Don't want photos? Tap 'Hide Photos' in the top bar above, or tap the button below to expand your Calendar and Tasks to full screen.",
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
    onHide: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(NernalColors.Card)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: Prev, AutoPlay, and [Hide]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onPrev,
                enabled = total > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous Photo",
                    tint = NernalColors.Ink,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(
                onClick = onToggleAuto,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (autoAdvance) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (autoAdvance) "Pause Slideshow" else "Play Slideshow",
                    tint = NernalColors.InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (onHide != null) {
                Surface(
                    color = NernalColors.CardSubtle,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onHide),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Hide",
                            color = NernalColors.InkMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Center: Photo Album & Year
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
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

        // Right: Star On/Off Filter Button, Favorite Current Photo Button, Next
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // "Star on" / "Star off" filter toggle
            Surface(
                color = if (favoritesOnly) Color(0xFF33260F) else NernalColors.CardSubtle,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (favoritesOnly) NernalColors.Amber else NernalColors.Border,
                ),
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleFavoritesFilter),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Star filter toggle",
                        tint = if (favoritesOnly) NernalColors.Amber else NernalColors.InkMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (favoritesOnly) "Star on" else "Star off",
                        color = if (favoritesOnly) NernalColors.Amber else NernalColors.InkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Star/Favorite Current Photo
            IconButton(
                onClick = onToggleStar,
                enabled = photo != null,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = if (isStarred) "Remove Favorite" else "Favorite Photo",
                    tint = if (isStarred) NernalColors.Amber else NernalColors.InkMuted,
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(
                onClick = onNext,
                enabled = total > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next Photo",
                    tint = NernalColors.Ink,
                    modifier = Modifier.size(28.dp),
                )
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
                    text = "Tap anywhere to wake Onnkiosk",
                    color = NernalColors.InkMuted.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                )
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
    onCalendarViewModeChange: (CalendarViewMode) -> Unit = {},
    onCalendarPrimaryModeChange: (Boolean) -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
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

                // Shortcut to Device Wi-Fi Settings
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenWifiSettings),
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
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Device Wi-Fi Settings",
                                    color = NernalColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Connect to Wi-Fi networks or change Wi-Fi settings",
                                    color = NernalColors.InkMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        Text(
                            text = "Open Wi-Fi →",
                            color = NernalColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
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
                    Text(
                        text = "Manage Imported Photo Albums",
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
                                    "4. Onnkiosk will auto-detect the link, import all photos into your slideshow rotation, and automatically complete the task!",
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
                        text = "Onnkiosk User Guide",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NernalColors.Ink,
                    )
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
                        title = "1. Overview & Navigation",
                        content = "Onnkiosk is an ambient touchscreen household dashboard signed directly into your Google account on-device. Household members at the counter never need to log in.\n" +
                            "• Left Wing: Live clock, precision local weather, NWS severe weather alerts, Doppler radar, and Home Hub (calendar & tasks).\n" +
                            "• Right Wing: High-resolution photo slideshow, favorite memories, and frame controls."
                    )

                    UserGuideSection(
                        title = "2. Clock, Weather & Doppler Radar",
                        content = "• The top card shows time, date, temperature, rain chance, and a 4-day forecast. Active NWS weather warnings appear as a clickable Alert pill.\n" +
                            "• Tap 'Radar' in the weather card to open animated pan/zoom Doppler radar with frame scrubbing."
                    )

                    UserGuideSection(
                        title = "3. Home Hub: Calendar & Tasks",
                        content = "• All Tab: Chronological list of upcoming events and active tasks.\n" +
                            "• Events Tab: Tap 'Events' to switch between Itinerary, 3-Day, Week, and Month views. Tap any event for details.\n" +
                            "• Tasks Tab: Focused view for your Google Task lists with checkboxes to mark items done.\n" +
                            "• Add (+) Button: Tap the '+' button in the top right by Tasks to quickly add a Task or Event.\n" +
                            "• Filter Dropdown: Filter individual task lists per tab, and use the 5-second Undo bar if you delete an item by mistake."
                    )

                    UserGuideSection(
                        title = "4. Quick-Add (+) Button",
                        content = "• Tap the '+' button in the top right to open the Add Entry dialog.\n" +
                            "• Select Task or Event, type the title or description, and tap Add.\n" +
                            "• Events sync directly to your primary Google Calendar and Tasks sync directly to your active Google Task list."
                    )

                    UserGuideSection(
                        title = "5. Photo Slideshow, Swiping & Favorites",
                        content = "• Swipe left or right on the photo stage (normal or fullscreen) to browse, or tap a photo to inspect fullscreen for 10 seconds.\n" +
                            "• Tap the Star (★) button to mark Favorites, 'Favorites' in the top bar to cycle starred photos, or Fullscreen for dedicated 30-minute viewing."
                    )

                    UserGuideSection(
                        title = "6. Google Photos: Picker & Shared Links",
                        content = "• Google Photos Picker: In Google Account → Photos, launch Picker to securely select specific photos straight from your library (cached locally on-frame).\n" +
                            "• Shared Links: Add a photos.app.goo.gl album link directly in Google Account, OR include the link in a Google Task title/notes and Onnkiosk will import it automatically."
                    )

                    UserGuideSection(
                        title = "7. Screen Dimming, Brightness & Event Alerts",
                        content = "• Tap 'Dim Screen' for ambient night mode showing time, weather, and the next upcoming event. Tap anywhere to wake back up.\n" +
                            "• Adjust Screen Brightness and Night Dim levels in Settings.\n" +
                            "• 'Wake on Event Reminder' alerts 1–5 minutes before a scheduled event and automatically closes once the event time starts."
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
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
