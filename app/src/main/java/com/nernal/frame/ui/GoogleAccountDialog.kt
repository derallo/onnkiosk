package com.nernal.frame.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.UserLocation

private enum class AccountTab(val label: String, val icon: ImageVector) {
    CALENDARS("Calendars", Icons.Rounded.CalendarMonth),
    TASKS("Task Lists", Icons.Rounded.Checklist),
    PHOTOS("Photo Albums", Icons.Rounded.PhotoLibrary),
    LOCATION("Location", Icons.Rounded.LocationOn),
}

@Composable
fun GoogleAccountDialog(
    state: NernalUiState,
    onStartSignIn: (openInAppWebView: Boolean) -> Unit,
    onSubmitCodeOrUrl: (String) -> Unit,
    onSignOut: () -> Unit,
    onSaveOAuthConfig: (clientId: String, clientSecret: String) -> Unit,
    onSyncNow: () -> Unit,
    onToggleCalendar: (String) -> Unit,
    onToggleTaskList: (String) -> Unit,
    onCreateTaskList: (title: String) -> Unit,
    onLaunchPicker: () -> Unit,
    onAddCustomSharedUrl: (String) -> Unit,
    onRemoveCustomSharedUrl: (String) -> Unit,
    onSearchLocations: (String) -> Unit,
    onSelectLocation: (UserLocation) -> Unit,
    onAutoDetectLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    var activeTab by remember { mutableStateOf(AccountTab.CALENDARS) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.88f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NernalColors.CardInner)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = if (state.authState == GoogleAuthState.SIGNED_IN) NernalColors.Emerald else NernalColors.Gold,
                            modifier = Modifier.size(32.dp),
                        )
                        Column {
                            Text(
                                text = if (state.authState == GoogleAuthState.SIGNED_IN) {
                                    state.accountProfile?.displayName ?: "Google Account"
                                } else {
                                    "Google Account & Sources"
                                },
                                color = NernalColors.Ink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when (state.authState) {
                                    GoogleAuthState.SIGNED_IN -> state.accountProfile?.email ?: "Signed in"
                                    GoogleAuthState.SIGNING_IN -> "Waiting for Google sign-in..."
                                    GoogleAuthState.ERROR -> state.authError ?: "Sign-in error"
                                    GoogleAuthState.NOT_SIGNED_IN -> "Sign in with Google to load your calendars, tasks, and photo albums"
                                },
                                color = if (state.authState == GoogleAuthState.ERROR) NernalColors.Rose else NernalColors.InkMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.authState == GoogleAuthState.SIGNED_IN) {
                            OutlinedButton(
                                onClick = onSyncNow,
                                enabled = !state.googleSyncing,
                                border = BorderStroke(1.dp, NernalColors.Border),
                                modifier = Modifier.height(48.dp),
                            ) {
                                if (state.googleSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NernalColors.Accent, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Refresh, null, tint = NernalColors.Gold, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", color = NernalColors.Ink, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                border = BorderStroke(1.dp, NernalColors.Rose.copy(alpha = 0.5f)),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Icon(Icons.Rounded.Logout, null, tint = NernalColors.Rose, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", color = NernalColors.Rose, fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { openDeviceWifiSettings(context) },
                            border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.5f)),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Wifi, null, tint = NernalColors.Accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wi-Fi Settings", color = NernalColors.Ink, fontSize = 13.sp)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, "Close", tint = NernalColors.Ink)
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                if (state.authState != GoogleAuthState.SIGNED_IN) {
                    SignedOutPanel(
                        state = state,
                        onStartInApp = { onStartSignIn(true) },
                        onStartSystemBrowser = {
                            val active = state.activeAuthUrl
                            if (active != null) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, active.toUri()))
                                }.onFailure {
                                    Toast.makeText(context, "No browser installed — use In-App Browser or Companion URL", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                onStartSignIn(false)
                            }
                        },
                        onOpenActiveUrlInBrowser = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            }.onFailure {
                                Toast.makeText(context, "No browser installed — open link from another device", Toast.LENGTH_LONG).show()
                            }
                        },
                        onSubmitCodeOrUrl = onSubmitCodeOrUrl,
                        onSaveOAuthConfig = onSaveOAuthConfig,
                        onSearchLocations = onSearchLocations,
                        onSelectLocation = onSelectLocation,
                        onAutoDetectLocation = onAutoDetectLocation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    // 4-tab selector bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NernalColors.CardInner)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (tab in AccountTab.entries) {
                            val selected = tab == activeTab
                            Surface(
                                onClick = { activeTab = tab },
                                color = if (selected) NernalColors.Gold else NernalColors.Card,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) NernalColors.Gold else NernalColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = if (selected) NernalColors.Bg else NernalColors.Gold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        color = if (selected) NernalColors.Bg else NernalColors.Ink,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = NernalColors.BorderSubtle)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        when (activeTab) {
                            AccountTab.CALENDARS -> CalendarsTabContent(
                                state = state,
                                onToggleCalendar = onToggleCalendar,
                            )
                            AccountTab.TASKS -> TaskListsTabContent(
                                state = state,
                                onToggleTaskList = onToggleTaskList,
                                onCreateTaskList = onCreateTaskList,
                            )
                            AccountTab.PHOTOS -> PhotoAlbumsTabContent(
                                state = state,
                                onLaunchPicker = onLaunchPicker,
                                onOpenPickerUrl = { uri ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
                                    }.onFailure {
                                        Toast.makeText(context, "No browser installed — open Picker URL on another device", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onAddSharedUrl = onAddCustomSharedUrl,
                                onRemoveSharedUrl = onRemoveCustomSharedUrl,
                            )
                            AccountTab.LOCATION -> LocationTabContent(
                                state = state,
                                onSearchLocations = onSearchLocations,
                                onSelectLocation = onSelectLocation,
                                onAutoDetectLocation = onAutoDetectLocation,
                            )
                        }
                    }
                }

                state.syncNotice?.let { notice ->
                    HorizontalDivider(color = NernalColors.BorderSubtle)
                    Text(
                        text = notice,
                        color = NernalColors.WarmAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SignedOutPanel(
    state: NernalUiState,
    onStartInApp: () -> Unit,
    onStartSystemBrowser: () -> Unit,
    onOpenActiveUrlInBrowser: (String) -> Unit,
    onSubmitCodeOrUrl: (String) -> Unit,
    onSaveOAuthConfig: (String, String) -> Unit,
    onSearchLocations: (String) -> Unit,
    onSelectLocation: (UserLocation) -> Unit,
    onAutoDetectLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var manualInput by remember { mutableStateOf("") }
    var showClientConfig by remember { mutableStateOf(state.oauthClientId.isBlank()) }
    var draftClientId by remember(state.oauthClientId) { mutableStateOf(state.oauthClientId) }
    var draftClientSecret by remember(state.oauthClientSecret) { mutableStateOf(state.oauthClientSecret) }
    var showLocationBox by remember { mutableStateOf(false) }

    val setupScrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(setupScrollState)
            .verticalScrollbar(setupScrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!state.authError.isNullOrBlank()) {
            Surface(
                color = Color(0xFF3B1D1D),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.Rose.copy(alpha = 0.6f)),
            ) {
                Text(
                    text = state.authError,
                    color = NernalColors.Rose,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Connect Your Google Account",
                    color = NernalColors.Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Sign in to select which Google Calendars, Google Task Lists, and Google Photo Albums appear on this display.",
                    color = NernalColors.InkMuted,
                    fontSize = 14.sp,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartSystemBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, null, tint = NernalColors.Bg, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.authState == GoogleAuthState.SIGNING_IN) "Re-open Sign-In in Browser" else "Sign in with Google",
                            color = NernalColors.Bg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!state.lanHelperUrl.isNullOrBlank()) {
                    Surface(
                        color = NernalColors.Bg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Phone / Laptop Companion Sign-In",
                                color = NernalColors.Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Open ${state.lanHelperUrl} on your phone or laptop on the same Wi-Fi. It forwards you to Google and includes a zero-typing paste form to send the redirect URL back automatically.",
                                color = NernalColors.InkMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = { Text("Paste Authorization Code or Redirect URL") },
                        singleLine = true,
                        colors = oauthTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onSubmitCodeOrUrl(manualInput)
                            manualInput = ""
                        },
                        enabled = manualInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text("Connect", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expandable OAuth Client Config
        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { showClientConfig = !showClientConfig },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "OAuth Client Config",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (showClientConfig) "Hide" else if (state.oauthClientId.isBlank()) "Required" else "Configured",
                        color = if (state.oauthClientId.isBlank()) NernalColors.Amber else NernalColors.Emerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (showClientConfig) {
                    OutlinedTextField(
                        value = draftClientId,
                        onValueChange = { draftClientId = it },
                        label = { Text("Google OAuth Client ID") },
                        singleLine = true,
                        colors = oauthTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draftClientSecret,
                        onValueChange = { draftClientSecret = it },
                        label = { Text("Client Secret (optional for PKCE Installed clients)") },
                        singleLine = true,
                        colors = oauthTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onSaveOAuthConfig(draftClientId, draftClientSecret) },
                        enabled = draftClientId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Save OAuth Client", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expandable Location quick-config even before sign-in
        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { showLocationBox = !showLocationBox },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Weather & Radar Location (${state.userLocation.name})",
                        color = NernalColors.Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (showLocationBox) "Hide" else "Change",
                        color = NernalColors.Accent,
                        fontSize = 13.sp,
                    )
                }

                if (showLocationBox) {
                    LocationTabContent(
                        state = state,
                        onSearchLocations = onSearchLocations,
                        onSelectLocation = onSelectLocation,
                        onAutoDetectLocation = onAutoDetectLocation,
                        scrollable = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarsTabContent(
    state: NernalUiState,
    onToggleCalendar: (String) -> Unit,
) {
    val calendarsScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(calendarsScrollState)
            .verticalScrollbar(calendarsScrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Choose which Google Calendars appear in Today's Agenda and the Events tab.",
            color = NernalColors.InkMuted,
            fontSize = 14.sp,
        )

        state.calendarError?.let { err ->
            Text(text = err, color = NernalColors.Rose, fontSize = 13.sp)
        }

        if (state.availableCalendars.isEmpty()) {
            Text(
                text = if (state.googleSyncing) "Loading calendars..." else "No calendars found on this account.",
                color = NernalColors.InkMuted,
                fontSize = 14.sp,
            )
        } else {
            for (cal in state.availableCalendars) {
                val checked = cal.id in state.selectedCalendarIds
                val dotColor = parseHexColorOr(cal.backgroundColor, NernalColors.Accent)
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (checked) NernalColors.Gold.copy(alpha = 0.65f) else NernalColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCalendar(cal.id) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (checked) NernalColors.Gold else NernalColors.InkMuted,
                            modifier = Modifier.size(26.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cal.summary,
                                color = NernalColors.Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = buildString {
                                    if (cal.primary) append("Primary · ")
                                    append(if (cal.isWritable) "Read & Write" else "Read Only")
                                },
                                color = NernalColors.InkMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListsTabContent(
    state: NernalUiState,
    onToggleTaskList: (String) -> Unit,
    onCreateTaskList: (String) -> Unit,
) {
    var newListTitle by remember { mutableStateOf("") }

    val tasksScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(tasksScrollState)
            .verticalScrollbar(tasksScrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Select Google Task Lists to display on the Tasks tab.",
            color = NernalColors.InkMuted,
            fontSize = 14.sp,
        )

        state.tasksError?.let { err ->
            Text(text = err, color = NernalColors.Rose, fontSize = 13.sp)
        }

        if (state.availableTaskLists.isEmpty()) {
            Text(
                text = if (state.googleSyncing) "Loading task lists..." else "No Google Task lists found.",
                color = NernalColors.InkMuted,
                fontSize = 14.sp,
            )
        } else {
            for (list in state.availableTaskLists) {
                val checked = list.id in state.selectedTaskListIds
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (checked) NernalColors.Gold.copy(alpha = 0.65f) else NernalColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTaskList(list.id) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (checked) NernalColors.Gold else NernalColors.InkMuted,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = list.title,
                            color = NernalColors.Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = NernalColors.BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Create New Google Task List",
            color = NernalColors.Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newListTitle,
                onValueChange = { newListTitle = it },
                placeholder = { Text("e.g., Household Tasks") },
                singleLine = true,
                colors = oauthTextFieldColors(),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    onCreateTaskList(newListTitle)
                    newListTitle = ""
                },
                enabled = newListTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Amber),
                modifier = Modifier.height(50.dp),
            ) {
                Icon(Icons.Rounded.Add, null, tint = NernalColors.Bg, modifier = Modifier.size(18.dp))
                Text("Add List", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PhotoAlbumsTabContent(
    state: NernalUiState,
    onLaunchPicker: () -> Unit,
    onOpenPickerUrl: (String) -> Unit,
    onAddSharedUrl: (String) -> Unit,
    onRemoveSharedUrl: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var draftUrl by remember { mutableStateOf("") }

    val photosScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(photosScrollState)
            .verticalScrollbar(photosScrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Display your Google Photos by selecting photos with the Google Photos Picker or pasting shared album links.",
            color = NernalColors.InkMuted,
            fontSize = 14.sp,
        )

        // Google Photos Picker Card
        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Photos Picker",
                            color = NernalColors.Gold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Pick up to 2,000 photos directly from photos.google.com. When you tap Done, the browser closes and automatically returns here.",
                            color = NernalColors.InkMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Button(
                        onClick = {
                            val uri = state.pickerSessionUri
                            if (uri != null) onOpenPickerUrl(uri) else onLaunchPicker()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Accent),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = if (state.pickerSessionUri != null) "Open Picker" else "Launch Picker",
                            color = NernalColors.Bg,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (state.pickerPolling) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NernalColors.Accent, strokeWidth = 2.dp)
                        Text(
                            text = state.pickerSessionUri?.let { "Waiting for selection in Google Photos..." }
                                ?: "Creating Google Photos Picker session...",
                            color = NernalColors.WarmAmber,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = NernalColors.BorderSubtle)

        // Shared Album Links Card (Whole Albums)
        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Add Whole Albums by Link",
                    color = NernalColors.Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Because Google restricted browsing private albums directly in 2025, you can add any album by link: in Google Photos, open the album, tap Share → 'Create Link', and paste the photos.app.goo.gl/... link below. Onnkiosk will sync and rotate through all photos in the album automatically.",
                    color = NernalColors.InkMuted,
                    fontSize = 12.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draftUrl,
                        onValueChange = { draftUrl = it },
                        placeholder = { Text("https://photos.app.goo.gl/...") },
                        singleLine = true,
                        colors = oauthTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (clip.isNotBlank()) draftUrl = clip
                        },
                        modifier = Modifier.height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NernalColors.Gold),
                        border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            onAddSharedUrl(draftUrl)
                            draftUrl = ""
                        },
                        enabled = draftUrl.trim().startsWith("http"),
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        modifier = Modifier.height(50.dp),
                    ) {
                        Text("Add Link", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        for (url in state.customSharedAlbumUrls) {
            Surface(
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = url,
                        color = NernalColors.Ink,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemoveSharedUrl(url) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Remove link", tint = NernalColors.InkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationTabContent(
    state: NernalUiState,
    onSearchLocations: (String) -> Unit,
    onSelectLocation: (UserLocation) -> Unit,
    onAutoDetectLocation: () -> Unit,
    scrollable: Boolean = true,
) {
    var query by remember { mutableStateOf("") }

    val locationScrollState = rememberScrollState()
    val baseModifier = if (scrollable) {
        Modifier
            .fillMaxSize()
            .verticalScroll(locationScrollState)
            .verticalScrollbar(locationScrollState)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = baseModifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = NernalColors.CardInner,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ACTIVE LOCATION", color = NernalColors.InkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(state.userLocation.name, color = NernalColors.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(
                            java.util.Locale.US,
                            "%.3f, %.3f · %s",
                            state.userLocation.latitude,
                            state.userLocation.longitude,
                            state.userLocation.timezone,
                        ),
                        color = NernalColors.InkMuted,
                        fontSize = 12.sp,
                    )
                }
                OutlinedButton(
                    onClick = onAutoDetectLocation,
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Rounded.MyLocation, null, tint = NernalColors.Accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Detect", color = NernalColors.Ink, fontSize = 13.sp)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search city or postal code (e.g., Austin, TX)") },
                singleLine = true,
                colors = oauthTextFieldColors(),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onSearchLocations(query) },
                enabled = query.trim().length >= 2 && !state.locationSearching,
                colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                modifier = Modifier.height(50.dp),
            ) {
                if (state.locationSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NernalColors.Bg, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Search, null, tint = NernalColors.Bg, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
                }
            }
        }

        for (loc in state.locationSearchResults) {
            Surface(
                color = NernalColors.CardInner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLocation(loc) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(loc.name, color = NernalColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(loc.timezone, color = NernalColors.InkMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppOAuthWebViewDialog(
    authUrl: String,
    onRedirectCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Bg,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.90f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(NernalColors.Card)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sign in with Google",
                        color = NernalColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                        Text("Cancel", color = NernalColors.Gold, fontSize = 14.sp)
                    }
                }

                var submitted by remember(authUrl) { mutableStateOf(false) }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptCookie(true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Standard Chrome UA without "; wv" so Google OAuth permits sign-in on AOSP frames.
                            settings.userAgentString =
                                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (isLocalOAuthCallback(url)) {
                                        if (!submitted) {
                                            submitted = true
                                            view?.stopLoading()
                                            onRedirectCaptured(url)
                                        }
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?,
                                ) {
                                    if (url != null && isLocalOAuthCallback(url)) {
                                        view?.stopLoading()
                                        if (!submitted) {
                                            submitted = true
                                            onRedirectCaptured(url)
                                        }
                                    }
                                }
                            }
                            loadUrl(authUrl)
                        }
                    },
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

private fun isLocalOAuthCallback(url: String): Boolean {
    val isLocal = url.startsWith("http://localhost:") || url.startsWith("http://127.0.0.1:")
    return isLocal && (url.contains("code=") || url.contains("error="))
}

@Composable
private fun oauthTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = NernalColors.Bg,
    unfocusedContainerColor = NernalColors.Bg,
    focusedBorderColor = NernalColors.Gold,
    unfocusedBorderColor = NernalColors.Border,
    focusedTextColor = NernalColors.Ink,
    unfocusedTextColor = NernalColors.Ink,
)

private fun parseHexColorOr(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching { Color(hex.toColorInt()) }.getOrDefault(fallback)
}
