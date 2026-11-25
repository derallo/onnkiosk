package com.nernal.frame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object NernalColors {
    // Pure black surfaces behind text for maximum OLED/IPS contrast
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF000000)
    val CardSubtle = Color(0xFF141414)
    val CardInner = Color(0xFF000000)
    val Border = Color(0xFF262626)
    val BorderSubtle = Color(0xFF1A1A1A)
    val Ink = Color(0xFFFAF8F5)
    val InkMuted = Color(0xFFBBB7AF)
    val Gold = Color(0xFFF5F2EB)
    val WarmAmber = Color(0xFFE7E1D6)
    val Accent = Color(0xFF60A5FA)
    val Glow = Color(0xFF2563EB)
    val Emerald = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Rose = Color(0xFFFB7185)
}

private val NernalColorScheme = darkColorScheme(
    primary = NernalColors.Accent,
    onPrimary = Color(0xFF082F49),
    secondary = NernalColors.Gold,
    onSecondary = NernalColors.Bg,
    background = NernalColors.Bg,
    onBackground = NernalColors.Ink,
    surface = NernalColors.Card,
    onSurface = NernalColors.Ink,
    surfaceVariant = NernalColors.CardSubtle,
    onSurfaceVariant = NernalColors.InkMuted,
    outline = NernalColors.Border,
    error = NernalColors.Rose,
)

@Composable
fun NernalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NernalColorScheme,
        content = content,
    )
}

@Composable
fun NernalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NernalColors.Card,
    borderColor: Color = NernalColors.Border,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = NernalColors.Ink,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, borderColor),
        content = content,
    )
}

@Composable
fun QrCodeImage(
    content: String,
    size: Dp = 112.dp,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(content) {
        runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        }.getOrNull()
    }
    if (matrix != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(6.dp),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cellW = this.size.width / matrix.width.toFloat()
                val cellH = this.size.height / matrix.height.toFloat()
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        if (matrix[x, y]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}
