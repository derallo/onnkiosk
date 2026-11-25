package com.nernal.frame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.BuildConfig
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.MemoryEntry
import com.nernal.frame.data.PiConnectionState
import com.nernal.frame.data.QuickAnswerCardData
import com.nernal.frame.data.VoiceAssistantState
import java.util.Locale

@Composable
fun VoiceSubtitlesOverlay(
    state: NernalUiState,
    onStopAndClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state.voiceState != VoiceAssistantState.IDLE || !state.voiceError.isNullOrBlank()
    AnimatedVisibility(
        visible = active && !state.isDimmed,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val statusColor = when (state.voiceState) {
            VoiceAssistantState.LISTENING -> NernalColors.Emerald
            VoiceAssistantState.THINKING -> NernalColors.Accent
            VoiceAssistantState.SPEAKING -> NernalColors.Gold
            VoiceAssistantState.IDLE -> NernalColors.Amber
        }
        val statusLabel = when (state.voiceState) {
            VoiceAssistantState.LISTENING -> if (state.isFollowUpListening) "Listening for follow-up..." else "Listening..."
            VoiceAssistantState.THINKING -> if (state.geminiApiKey.isNotBlank()) "Thinking with Gemini..." else "Processing..."
            VoiceAssistantState.SPEAKING -> "Speaking..."
            VoiceAssistantState.IDLE -> "Voice Notice"
        }

        Surface(
            color = Color(0xEE10141C),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.75f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .padding(top = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.voiceState == VoiceAssistantState.THINKING) {
                                CircularProgressIndicator(
                                    color = statusColor,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.voiceState == VoiceAssistantState.SPEAKING) {
                                        Icons.Rounded.VolumeUp
                                    } else {
                                        Icons.Rounded.Mic
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.voiceUserTranscript.isNotBlank()) {
                            Text(
                                text = "You: \"${state.voiceUserTranscript}\"",
                                color = NernalColors.Ink,
                                fontSize = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (state.voiceAssistantTranscript.isNotBlank()) {
                            Text(
                                text = state.voiceAssistantTranscript,
                                color = NernalColors.WarmAmber,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!state.voiceError.isNullOrBlank()) {
                            Text(
                                text = state.voiceError,
                                color = NernalColors.Amber,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Surface(
                    onClick = onStopAndClose,
                    color = NernalColors.CardSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss Voice Overlay",
                            tint = NernalColors.Ink,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAnswerCardOverlay(
    card: QuickAnswerCardData?,
    canSpeak: Boolean,
    isSpeaking: Boolean,
    onReadAloud: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = card != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (card != null) {
            Surface(
                color = Color(0xF5121620),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, NernalColors.Gold.copy(alpha = 0.7f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .padding(bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
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
                                text = card.question,
                                color = NernalColors.Gold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canSpeak) {
                                OutlinedButton(
                                    onClick = { if (isSpeaking) onStopSpeaking() else onReadAloud(card.answer) },
                                    border = BorderStroke(1.dp, NernalColors.Border),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                        contentDescription = null,
                                        tint = NernalColors.Gold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isSpeaking) "Stop" else "Read Aloud",
                                        color = NernalColors.Ink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Surface(
                                onClick = onDismiss,
                                color = NernalColors.CardSubtle,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NernalColors.Border),
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close Answer Card",
                                        tint = NernalColors.Ink,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = card.answer,
                        color = NernalColors.Ink,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryBookDialog(
    entries: List<MemoryEntry>,
    canSpeak: Boolean,
    onReadAloud: (String) -> Unit,
    onDeleteLocal: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedNotebook by remember { mutableStateOf<String?>(null) }
    val notebooks = remember(entries) {
        entries.map { it.notebook }.distinct().sorted()
    }
    val visibleEntries = remember(entries, selectedNotebook) {
        if (selectedNotebook == null) entries else entries.filter { it.notebook == selectedNotebook }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.86f),
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
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                text = "Memory Book & Recipes",
                                color = NernalColors.Ink,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${entries.size} saved note${if (entries.size == 1) "" else "s"}",
                                color = NernalColors.InkMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Close", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                if (notebooks.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MemoryFilterChip(
                            label = "All",
                            selected = selectedNotebook == null,
                            onClick = { selectedNotebook = null },
                        )
                        for (nb in notebooks) {
                            MemoryFilterChip(
                                label = nb.replaceFirstChar { it.uppercase(Locale.US) },
                                selected = selectedNotebook == nb,
                                onClick = { selectedNotebook = nb },
                            )
                        }
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .verticalScrollbar(scroll)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (visibleEntries.isEmpty()) {
                        Surface(
                            color = NernalColors.CardInner,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "No memories saved yet",
                                    color = NernalColors.Ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Enter a Gemini API key in Settings and tell the assistant things to remember — like family recipes, birthdays, doctor numbers, or household notes" +
                                        if (BuildConfig.IS_NERNAL_FAMILY_BUILD) " — or link to a Nernal Raspberry Pi." else ".",
                                    color = NernalColors.InkMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                )
                            }
                        }
                    } else {
                        for (entry in visibleEntries) {
                            Surface(
                                color = NernalColors.CardInner,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.notebook.uppercase(Locale.US) + if (entry.fromPi) " · PI" else "",
                                                color = NernalColors.Gold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp,
                                            )
                                            Text(
                                                text = entry.title,
                                                color = NernalColors.Ink,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (canSpeak) {
                                                IconButton(
                                                    onClick = { onReadAloud("${entry.title}. ${entry.content}") },
                                                    modifier = Modifier.size(48.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.VolumeUp,
                                                        contentDescription = "Read Memory Aloud",
                                                        tint = NernalColors.Gold,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                            if (!entry.fromPi) {
                                                IconButton(
                                                    onClick = { onDeleteLocal(entry.id) },
                                                    modifier = Modifier.size(48.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Delete Memory",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = entry.content.ifBlank { entry.summary },
                                        color = NernalColors.Ink,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    )
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
private fun MemoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) NernalColors.Gold else NernalColors.CardInner,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) NernalColors.Gold else NernalColors.Border),
        modifier = Modifier
            .height(44.dp)
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
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
fun PiSyncDialog(
    state: NernalUiState,
    onSyncNow: (String) -> Unit,
    onRebuildLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftUrl by remember(state.piBaseUrl) { mutableStateOf(state.piBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NernalColors.Card,
        titleContentColor = NernalColors.Ink,
        textContentColor = NernalColors.Ink,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = NernalColors.Gold)
                Text("Pi Sync & Photo Library", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val statusSummary = when (state.piConnection) {
                    PiConnectionState.CONNECTED -> "Connected to Nernal Pi (${state.activePiUrl}). Agenda, lists, Memory Book, and ★ favorites sync automatically."
                    PiConnectionState.CHECKING -> "Checking connection to Nernal Pi..."
                    PiConnectionState.STANDALONE -> "Running standalone (Pi not reachable on this network). Edits stay queued locally until back on the home LAN."
                }
                Text(statusSummary, color = NernalColors.WarmAmber, fontSize = 14.sp)

                if (state.pendingSyncCount > 0) {
                    Text(
                        text = "${state.pendingSyncCount} local change(s) queued for next sync.",
                        color = NernalColors.Amber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = if (state.libraryTotalCount > 0) {
                        "Photo library (${state.libraryTotalCount} photos) is cached locally on this device."
                    } else {
                        "Shared albums and Pi photos load automatically when linked."
                    },
                    color = NernalColors.InkMuted,
                    fontSize = 13.sp,
                )

                OutlinedTextField(
                    value = draftUrl,
                    onValueChange = { draftUrl = it },
                    label = { Text("Pi Address (default 192.168.68.86:3000)") },
                    singleLine = true,
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

                state.syncNotice?.let {
                    Text(it, color = NernalColors.WarmAmber, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSyncNow(draftUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                    ) {
                        Text("Test & Sync Pi", color = NernalColors.Bg, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRebuildLibrary,
                        enabled = !state.albumsSyncing,
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = if (state.albumsSyncing) "Rebuilding..." else "Rebuild Photo Index",
                            color = NernalColors.Gold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Close", color = NernalColors.Gold, fontSize = 15.sp)
            }
        },
    )
}
