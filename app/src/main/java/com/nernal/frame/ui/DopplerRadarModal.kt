package com.nernal.frame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.RadarData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val TILE_DP = 256f

@Composable
fun DopplerRadarModal(
    homeLat: Double,
    homeLon: Double,
    locationLabel: String,
    zoneId: ZoneId,
    radarData: RadarData?,
    isLoading: Boolean,
    loadTile: suspend (url: String, alpha: Boolean) -> Bitmap?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var zoom by remember { mutableIntStateOf(7) }
    var centerTileX by remember(homeLat, homeLon) { mutableDoubleStateOf(NernalRepository.lonToTileX(homeLon, 7)) }
    var centerTileY by remember(homeLat, homeLon) { mutableDoubleStateOf(NernalRepository.latToTileY(homeLat, 7)) }
    var frameIdx by remember(radarData) {
        val lastPast = radarData?.frames?.indexOfLast { !it.isForecast } ?: -1
        mutableIntStateOf(if (lastPast >= 0) lastPast else 0)
    }
    var isPlaying by remember { mutableStateOf(true) }

    val baseTiles = remember { mutableStateMapOf<String, Bitmap>() }
    val radarTiles = remember { mutableStateMapOf<String, Bitmap>() }

    val frames = radarData?.frames.orEmpty()
    val safeFrameIdx = if (frames.isNotEmpty()) frameIdx.coerceIn(0, frames.lastIndex) else 0
    val currentFrame = frames.getOrNull(safeFrameIdx)

    // Smooth radar animation loop
    LaunchedEffect(isPlaying, frames.size) {
        while (isPlaying && frames.size > 1) {
            delay(750L)
            frameIdx = (frameIdx + 1) % frames.size
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NernalColors.Bg,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(NernalColors.Card)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Live Doppler Radar · ${locationLabel.ifBlank { "Local" }}",
                            color = NernalColors.Ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        currentFrame?.let { f ->
                            val timeStr = Instant.ofEpochSecond(f.timeSec)
                                .atZone(zoneId)
                                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
                            val sourceTag = if (f.isForecast) {
                                "Forecast"
                            } else if (f.path.startsWith("/nexrad-")) {
                                "NEXRAD"
                            } else {
                                "Radar"
                            }
                            Surface(
                                color = if (f.isForecast) Color(0xFF1E3A5F) else Color(0xFF14332A),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (f.isForecast) NernalColors.Accent else NernalColors.Emerald,
                                ),
                            ) {
                                Text(
                                    text = "$sourceTag · $timeStr",
                                    color = if (f.isForecast) NernalColors.Accent else NernalColors.Emerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NernalColors.Accent)
                            } else {
                                Icon(Icons.Rounded.Refresh, "Refresh Radar", tint = NernalColors.Ink)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, "Close Radar", tint = NernalColors.Ink)
                        }
                    }
                }

                // Map Canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0D12))
                        .clipToBounds()
                        .pointerInput(zoom) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val tilePx = TILE_DP * density
                                val maxTiles = (1 shl zoom).toDouble()
                                centerTileX = (centerTileX - dragAmount.x / tilePx).coerceIn(0.0, maxTiles)
                                centerTileY = (centerTileY - dragAmount.y / tilePx).coerceIn(0.5, maxTiles - 0.5)
                            }
                        },
                ) {
                    val densityPx = LocalDensity.current.density
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val tilePx = TILE_DP * densityPx

                    val tilesLeft = floor(centerTileX - (widthPx / 2f) / tilePx).toInt()
                    val tilesRight = ceil(centerTileX + (widthPx / 2f) / tilePx).toInt()
                    val tilesTop = floor(centerTileY - (heightPx / 2f) / tilePx).toInt()
                    val tilesBottom = ceil(centerTileY + (heightPx / 2f) / tilePx).toInt()
                    val n = 1 shl zoom

                    val visibleCoords = remember(zoom, tilesLeft, tilesRight, tilesTop, tilesBottom) {
                        buildList {
                            for (ty in tilesTop..tilesBottom) {
                                if (ty !in 0 until n) continue
                                for (txRaw in tilesLeft..tilesRight) {
                                    val tx = ((txRaw % n) + n) % n
                                    add(Triple(txRaw, tx, ty))
                                }
                            }
                        }
                    }

                    // Load dark ArcGIS basemap tiles independently so radar frame updates never cancel them
                    LaunchedEffect(zoom, visibleCoords) {
                        val keepKeys = visibleCoords.map { (_, tx, ty) -> "$zoom/$tx/$ty" }.toSet()
                        baseTiles.keys.toList().forEach { if (it !in keepKeys) baseTiles.remove(it) }
                        visibleCoords.map { (_, tx, ty) ->
                            async {
                                val baseKey = "$zoom/$tx/$ty"
                                if (baseKey !in baseTiles) {
                                    val baseUrl =
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/$zoom/$ty/$tx"
                                    loadTile(baseUrl, false)?.let { baseTiles[baseKey] = it }
                                }
                            }
                        }.awaitAll()
                    }

                    // Preload radar animation frames sequentially per frame (active frame first)
                    LaunchedEffect(zoom, visibleCoords, radarData) {
                        val keepRadarKeys = buildSet {
                            for (frame in frames) {
                                for ((_, tx, ty) in visibleCoords) {
                                    add("${frame.path}/$zoom/$tx/$ty")
                                }
                            }
                        }
                        radarTiles.keys.toList().forEach { if (it !in keepRadarKeys) radarTiles.remove(it) }
                        if (radarData != null && frames.isNotEmpty()) {
                            val orderedFrames = listOfNotNull(frames.getOrNull(safeFrameIdx)) +
                                frames.filterIndexed { i, _ -> i != safeFrameIdx }
                            for (frame in orderedFrames) {
                                visibleCoords.map { (_, tx, ty) ->
                                    async {
                                        val rKey = "${frame.path}/$zoom/$tx/$ty"
                                        if (rKey !in radarTiles) {
                                            val rUrl = NernalRepository.radarTileUrl(
                                                radarData.host,
                                                frame.path,
                                                zoom,
                                                tx,
                                                ty,
                                            )
                                            loadTile(rUrl, true)?.let { radarTiles[rKey] = it }
                                        }
                                    }
                                }.awaitAll()
                            }
                        }
                    }

                    // Render Base + Radar tiles
                    for ((txRaw, tx, ty) in visibleCoords) {
                        key(zoom, txRaw, ty) {
                            val leftPx = (widthPx / 2f + (txRaw - centerTileX) * tilePx).roundToInt()
                            val topPx = (heightPx / 2f + (ty - centerTileY) * tilePx).roundToInt()
                            val tileSizeDp = (TILE_DP + 0.5f).dp

                            baseTiles["$zoom/$tx/$ty"]?.let { bmp ->
                                Image(
                                    bitmap = remember(bmp) { bmp.asImageBitmap() },
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .offset { IntOffset(leftPx, topPx) }
                                        .size(tileSizeDp),
                                )
                            }

                            currentFrame?.let { frame ->
                                radarTiles["${frame.path}/$zoom/$tx/$ty"]?.let { rBmp ->
                                    Image(
                                        bitmap = remember(rBmp) { rBmp.asImageBitmap() },
                                        contentDescription = null,
                                        alpha = 0.78f,
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier
                                            .offset { IntOffset(leftPx, topPx) }
                                            .size(tileSizeDp),
                                    )
                                }
                            }
                        }
                    }

                    // Home marker
                    val homeX = widthPx / 2f + ((NernalRepository.lonToTileX(homeLon, zoom) - centerTileX) * tilePx).toFloat()
                    val homeY = heightPx / 2f + ((NernalRepository.latToTileY(homeLat, zoom) - centerTileY) * tilePx).toFloat()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0x4038BDF8),
                            radius = 18f * density,
                            center = Offset(homeX, homeY),
                        )
                        drawCircle(
                            color = NernalColors.Card,
                            radius = 9f * density,
                            center = Offset(homeX, homeY),
                        )
                        drawCircle(
                            color = NernalColors.Gold,
                            radius = 6f * density,
                            center = Offset(homeX, homeY),
                        )
                    }

                    // Right-side Zoom + Recenter floating controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadarMapButton(Icons.Rounded.Add, "Zoom In") {
                            if (zoom < 8) {
                                val nextZ = zoom + 1
                                centerTileX *= 2.0
                                centerTileY *= 2.0
                                zoom = nextZ
                            }
                        }
                        RadarMapButton(Icons.Rounded.Remove, "Zoom Out") {
                            if (zoom > 5) {
                                val nextZ = zoom - 1
                                centerTileX /= 2.0
                                centerTileY /= 2.0
                                zoom = nextZ
                            }
                        }
                        RadarMapButton(Icons.Rounded.MyLocation, "Center Home") {
                            centerTileX = NernalRepository.lonToTileX(homeLon, zoom)
                            centerTileY = NernalRepository.latToTileY(homeLat, zoom)
                        }
                    }

                    if (radarData == null && isLoading) {
                        CircularProgressIndicator(
                            color = NernalColors.Accent,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                // Playback Scrubber Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .background(NernalColors.Card)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(50.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NernalColors.Ink,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            isPlaying = false
                            if (frames.isNotEmpty()) {
                                frameIdx = (safeFrameIdx - 1 + frames.size) % frames.size
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, "Previous Frame", tint = NernalColors.Ink)
                    }

                    Slider(
                        value = safeFrameIdx.toFloat(),
                        onValueChange = {
                            isPlaying = false
                            frameIdx = it.roundToInt().coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                        },
                        valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NernalColors.Accent,
                            activeTrackColor = NernalColors.Accent,
                            inactiveTrackColor = NernalColors.Border,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        onClick = {
                            isPlaying = false
                            if (frames.isNotEmpty()) {
                                frameIdx = (safeFrameIdx + 1) % frames.size
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.ChevronRight, "Next Frame", tint = NernalColors.Ink)
                    }

                    Text(
                        text = if (frames.isNotEmpty()) "${safeFrameIdx + 1} / ${frames.size}" else "--",
                        color = NernalColors.InkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        color = NernalColors.Card.copy(alpha = 0.92f),
        shape = CircleShape,
        border = BorderStroke(1.dp, NernalColors.Border),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, description, tint = NernalColors.Ink)
        }
    }
}
