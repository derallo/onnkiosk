package com.nernal.frame.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a visible, high-contrast vertical scrollbar for regular scrollable containers (e.g. Column with verticalScroll).
 */
fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 5.dp,
    paddingEnd: Dp = 2.dp,
    trackColor: Color = Color(0x33FFFFFF),
    thumbColor: Color = Color(0xCCFAF8F5),
): Modifier = this.drawWithContent {
    drawContent()
    if (scrollState.maxValue > 0) {
        val totalHeight = size.height
        val minThumb = 28.dp.toPx()
        if (totalHeight <= 0f) return@drawWithContent
        val contentHeight = totalHeight + scrollState.maxValue
        val thumbHeight = if (totalHeight <= minThumb) totalHeight else (totalHeight * (totalHeight / contentHeight)).coerceIn(minThumb, totalHeight)
        val scrollFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbTop = scrollFraction * (totalHeight - thumbHeight)

        val widthPx = width.toPx()
        val xOffset = size.width - widthPx - paddingEnd.toPx()
        val cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)

        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(xOffset, 0f),
            size = Size(widthPx, totalHeight),
            cornerRadius = cornerRadius,
        )

        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(xOffset, thumbTop),
            size = Size(widthPx, thumbHeight),
            cornerRadius = cornerRadius,
        )
    }
}

/**
 * Draws a visible, high-contrast vertical scrollbar for LazyColumn lists.
 */
fun Modifier.lazyListVerticalScrollbar(
    state: LazyListState,
    width: Dp = 5.dp,
    paddingEnd: Dp = 2.dp,
    trackColor: Color = Color(0x33FFFFFF),
    thumbColor: Color = Color(0xCCFAF8F5),
): Modifier = this.drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalCount = layoutInfo.totalItemsCount

    if (visibleItems.isNotEmpty() && totalCount > visibleItems.size) {
        val viewportHeight = size.height
        val minThumb = 28.dp.toPx()
        if (viewportHeight <= 0f) return@drawWithContent
        val fractionVisible = (visibleItems.size.toFloat() / totalCount.toFloat()).coerceIn(0.08f, 1f)
        val thumbHeight = if (viewportHeight <= minThumb) viewportHeight else (viewportHeight * fractionVisible).coerceIn(minThumb, viewportHeight)

        val firstItem = visibleItems.first()
        val firstItemFraction = if (firstItem.size > 0) {
            (-firstItem.offset.toFloat() / firstItem.size.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val exactIndex = firstItem.index + firstItemFraction
        val maxScrollableIndex = (totalCount - visibleItems.size).coerceAtLeast(1)
        val scrollFraction = (exactIndex / maxScrollableIndex.toFloat()).coerceIn(0f, 1f)
        val thumbTop = scrollFraction * (viewportHeight - thumbHeight)

        val widthPx = width.toPx()
        val xOffset = size.width - widthPx - paddingEnd.toPx()
        val cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)

        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(xOffset, 0f),
            size = Size(widthPx, viewportHeight),
            cornerRadius = cornerRadius,
        )

        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(xOffset, thumbTop),
            size = Size(widthPx, thumbHeight),
            cornerRadius = cornerRadius,
        )
    }
}

/**
 * Draws a visible horizontal scrollbar for horizontally scrollable rows (e.g. Row with horizontalScroll).
 */
fun Modifier.horizontalScrollbar(
    scrollState: ScrollState,
    height: Dp = 4.dp,
    paddingBottom: Dp = 2.dp,
    trackColor: Color = Color(0x33FFFFFF),
    thumbColor: Color = Color(0xCCFAF8F5),
): Modifier = this.drawWithContent {
    drawContent()
    if (scrollState.maxValue > 0) {
        val totalWidth = size.width
        val minThumb = 24.dp.toPx()
        if (totalWidth <= 0f) return@drawWithContent
        val contentWidth = totalWidth + scrollState.maxValue
        val thumbWidth = if (totalWidth <= minThumb) totalWidth else (totalWidth * (totalWidth / contentWidth)).coerceIn(minThumb, totalWidth)
        val scrollFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbLeft = scrollFraction * (totalWidth - thumbWidth)

        val heightPx = height.toPx()
        val yOffset = size.height - heightPx - paddingBottom.toPx()
        val cornerRadius = CornerRadius(heightPx / 2f, heightPx / 2f)

        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, yOffset),
            size = Size(totalWidth, heightPx),
            cornerRadius = cornerRadius,
        )

        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(thumbLeft, yOffset),
            size = Size(thumbWidth, heightPx),
            cornerRadius = cornerRadius,
        )
    }
}
