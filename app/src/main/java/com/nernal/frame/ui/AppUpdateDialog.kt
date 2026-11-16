package com.nernal.frame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.AppReleaseInfo

@Composable
fun AppUpdateDialog(
    release: AppReleaseInfo,
    state: NernalUiState,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
            if (!state.isDownloadingUpdate) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Gold),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = NernalColors.Gold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(44.dp),
                    ) {
                        BoxContentCenter {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "New Update Available",
                            color = NernalColors.Ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${release.tagName}${if (release.title.isNotBlank()) " · ${release.title}" else ""}",
                            color = NernalColors.Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Changelog / Release notes
                if (release.changelog.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "What's New:",
                            color = NernalColors.InkMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = release.changelog,
                            color = NernalColors.Ink,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                } else {
                    Text(
                        text = "A new version of Onnkiosk is available for installation.",
                        color = NernalColors.InkMuted,
                        fontSize = 14.sp,
                    )
                }

                // Error message if any
                if (!state.updateError.isNullOrBlank()) {
                    Text(
                        text = state.updateError,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                    )
                }

                // Progress Bar if Downloading
                if (state.isDownloadingUpdate) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val pct = (state.updateDownloadProgress * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Downloading update...", color = NernalColors.InkMuted, fontSize = 12.sp)
                            Text("$pct%", color = NernalColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { state.updateDownloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = NernalColors.Gold,
                            trackColor = NernalColors.CardSubtle,
                        )
                    }
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!state.isDownloadingUpdate) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NernalColors.Border),
                            modifier = Modifier.height(46.dp),
                        ) {
                            Text("Later", color = NernalColors.InkMuted, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Button(
                        onClick = onInstall,
                        enabled = !state.isDownloadingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp),
                    ) {
                        if (state.isDownloadingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NernalColors.Bg,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Downloading...", color = NernalColors.Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Download & Install", color = NernalColors.Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxContentCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}
