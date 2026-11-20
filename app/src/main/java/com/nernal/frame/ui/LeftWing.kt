package com.nernal.frame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FilterDrama
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nernal.frame.NernalUiState
import com.nernal.frame.data.CalendarViewMode
import com.nernal.frame.data.DailyForecast
import com.nernal.frame.data.GoogleAuthState
import com.nernal.frame.data.GoogleCalendarInfo
import com.nernal.frame.data.GoogleTaskListInfo
import com.nernal.frame.data.HubFilter
import com.nernal.frame.data.ReminderCategory
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.WeatherAlert
import com.nernal.frame.data.WeatherConditionKind
import com.nernal.frame.data.WeatherData
import com.nernal.frame.data.eventColor
import com.nernal.frame.data.parseTimeLabelToMinutes
import com.nernal.frame.data.selectNextUpcomingEvent
import com.nernal.frame.data.wmoKind
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun InformationalWing(
    state: NernalUiState,
    onRefreshWeather: () -> Unit,
    onOpenRadar: () -> Unit,
    onFilterChange: (HubFilter) -> Unit,
    onToggleActiveTaskListFilter: (String) -> Unit = {},
    onAddReminder: (String, ReminderCategory) -> Unit,
    onToggleReminder: (String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onEventClick: (ReminderItem) -> Unit,
    onCalendarViewModeChange: (CalendarViewMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UnifiedHeaderCard(
            locationName = state.userLocation.name,
            time = state.clockTime,
            amPm = state.clockAmPm,
            date = state.clockDate,
            todayKey = state.todayKey,
            reminders = state.reminders,
            weather = state.weather,
            weatherLoading = state.weatherLoading,
            weatherError = state.weatherError,
            onRefreshWeather = onRefreshWeather,
            onOpenRadar = onOpenRadar,
            onEventClick = onEventClick,
            modifier = Modifier.fillMaxWidth(),
        )

        OmniHubCard(
            reminders = state.reminders,
            activeFilter = state.hubFilter,
            calendarViewMode = state.calendarViewMode,
            todayKey = state.todayKey,
            availableTaskLists = state.availableTaskLists,
            selectedTaskListIds = state.selectedTaskListIds,
            activeTaskListFilters = state.activeTaskListFilters,
            availableCalendars = state.availableCalendars,
            onToggleActiveTaskListFilter = onToggleActiveTaskListFilter,
            onFilterChange = onFilterChange,
            onCalendarViewModeChange = onCalendarViewModeChange,
            onAddReminder = onAddReminder,
            onToggleReminder = onToggleReminder,
            onDeleteReminder = onDeleteReminder,
            onEventClick = onEventClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun UnifiedHeaderCard(
    locationName: String,
    time: String,
    amPm: String,
    date: String,
    todayKey: String,
    reminders: List<ReminderItem>,
    weather: WeatherData?,
    weatherLoading: Boolean,
    weatherError: String?,
    onRefreshWeather: () -> Unit,
    onOpenRadar: () -> Unit,
    onEventClick: (ReminderItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAlertsDialog by remember { mutableStateOf(false) }

    val tomorrowKey = remember(todayKey) {
        runCatching { LocalDate.parse(todayKey).plusDays(1).toString() }.getOrDefault("")
    }

    val nowMinutes = remember(time, amPm) {
        parseTimeLabelToMinutes("$time $amPm")
    }
    val nextEvent = remember(reminders, todayKey, nowMinutes) {
        selectNextUpcomingEvent(reminders, todayKey, nowMinutes)
    }

    val alerts = weather?.alerts.orEmpty()
    if (showAlertsDialog && alerts.isNotEmpty()) {
        WeatherAlertsDialog(
            alerts = alerts,
            onDismiss = { showAlertsDialog = false },
        )
    }

    NernalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ROW 1: Date & Town on Left, Weather Alerts + Radar + Refresh on Right
            val topHeaderLabel = remember(date, locationName) {
                val town = locationName.ifBlank { "LOCAL TIME" }
                if (date.isNotBlank()) "$date | $town" else town
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = topHeaderLabel,
                    color = NernalColors.InkMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (alerts.isNotEmpty()) {
                        Surface(
                            color = NernalColors.Rose.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NernalColors.Rose.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { showAlertsDialog = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Weather Alert",
                                    tint = NernalColors.Rose,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "${alerts.size} Alert${if (alerts.size > 1) "s" else ""}",
                                    color = NernalColors.Rose,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                        modifier = Modifier
                            .height(48.dp)
                            .clickable(onClick = onOpenRadar),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Radar",
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Radar",
                                color = NernalColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshWeather,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (weatherLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NernalColors.Accent,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = NernalColors.InkMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // ROW 2: PRIMARY TELEMETRY — Clock on Left, Current Day's Weather RIGHT BESIDE IT!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // LEFT: Time
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = time.ifEmpty { "--:--" },
                        color = NernalColors.Ink,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = amPm,
                        color = NernalColors.InkMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }

                // Vertical Divider between Time and Weather
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                        .background(NernalColors.BorderSubtle),
                )

                // RIGHT: Current Day's Weather right beside the time!
                if (weather != null) {
                    val (icon, tint) = weatherIconAndTint(weather.weatherCode, weather.isDay)
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = weather.description,
                            tint = tint,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(36.dp),
                        )
                        Text(
                            text = "${weather.currentTempF}°",
                            color = NernalColors.Ink,
                            fontSize = 38.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .weight(1f, fill = false),
                        ) {
                            Text(
                                text = weather.description,
                                color = NernalColors.Gold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Feels ${weather.apparentTempF}° · ${weather.humidityPercent}% · ${weather.windMph} mph",
                                color = NernalColors.InkMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = if (weatherLoading) "Loading weather..." else (weatherError ?: "Weather unavailable"),
                            color = NernalColors.InkMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // ROW 3: 4-day forecast chips
            if (weather != null && weather.daily.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (day in weather.daily.take(4)) {
                        ForecastDayChip(
                            day = day,
                            isToday = day.dayLabel == "Today",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalDivider(
                color = NernalColors.BorderSubtle,
                modifier = Modifier.padding(vertical = 2.dp),
            )

            // ROW 4: Next Event
            if (nextEvent != null) {
                val dateLabel = when (nextEvent.dateKey) {
                    todayKey -> "Today · $date"
                    tomorrowKey -> "Tomorrow · ${formatSectionDate(tomorrowKey)}"
                    else -> formatSectionDate(nextEvent.dateKey ?: "")
                }
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onEventClick(nextEvent) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                color = NernalColors.Accent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, NernalColors.Accent.copy(alpha = 0.45f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Event,
                                        contentDescription = null,
                                        tint = NernalColors.Accent,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = nextEvent.time ?: "All Day",
                                        color = NernalColors.Accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            Text(
                                text = nextEvent.text,
                                color = NernalColors.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val descSnippet = nextEvent.notes?.trim()?.replace('\n', ' ')?.takeIf { it.isNotBlank() }
                            if (descSnippet != null) {
                                Text(
                                    text = "— $descSnippet",
                                    color = NernalColors.InkMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }

                            Text(
                                text = "· $dateLabel",
                                color = NernalColors.WarmAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }

                        Text(
                            text = "Details →",
                            color = NernalColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Surface(
                    color = NernalColors.CardInner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = NernalColors.InkMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "No upcoming calendar events scheduled today · Relax and enjoy your day",
                            color = NernalColors.InkMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastDayChip(
    day: DailyForecast,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val (icon, tint) = weatherIconAndTint(day.weatherCode, isDay = true)
    Surface(
        modifier = modifier,
        color = NernalColors.CardInner,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isToday) NernalColors.Gold.copy(alpha = 0.45f) else NernalColors.BorderSubtle,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = day.dayLabel,
                color = if (isToday) NernalColors.Gold else NernalColors.InkMuted,
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${day.maxTempF}°",
                    color = NernalColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${day.minTempF}°",
                    color = NernalColors.InkMuted,
                    fontSize = 12.sp,
                )
                val (precipIcon, precipTint) = precipIconAndTint(day.weatherCode)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Icon(
                        imageVector = precipIcon,
                        contentDescription = null,
                        tint = if (day.precipChance >= 20) precipTint else NernalColors.InkMuted,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "${day.precipChance}%",
                        color = if (day.precipChance >= 20) NernalColors.Accent else NernalColors.InkMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OmniHubCard(
    reminders: List<ReminderItem>,
    activeFilter: HubFilter,
    calendarViewMode: CalendarViewMode = CalendarViewMode.ITINERARY,
    todayKey: String,
    availableTaskLists: List<GoogleTaskListInfo> = emptyList(),
    selectedTaskListIds: Set<String> = emptySet(),
    activeTaskListFilters: Set<String> = emptySet(),
    availableCalendars: List<GoogleCalendarInfo> = emptyList(),
    onToggleActiveTaskListFilter: (String) -> Unit = {},
    onFilterChange: (HubFilter) -> Unit,
    onCalendarViewModeChange: (CalendarViewMode) -> Unit = {},
    onAddReminder: (String, ReminderCategory) -> Unit,
    onToggleReminder: (String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onEventClick: (ReminderItem) -> Unit,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var eventsMenuExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val eventsListState = rememberLazyListState()
    val allListState = rememberLazyListState()
    val tasksListState = rememberLazyListState()

    val relevantTaskLists = remember(availableTaskLists, selectedTaskListIds) {
        val filtered = availableTaskLists.filter { it.id in selectedTaskListIds }
        if (filtered.isNotEmpty()) filtered else availableTaskLists
    }
    val tabFilterableLists = remember(relevantTaskLists, activeFilter) {
        if (activeFilter == HubFilter.TASKS) relevantTaskLists else emptyList()
    }

    val tomorrowKey = remember(todayKey) {
        runCatching { LocalDate.parse(todayKey).plusDays(1).toString() }.getOrDefault("")
    }

    val todayDateFormatted = remember(todayKey) { formatSectionDate(todayKey) }
    val tomorrowDateFormatted = remember(tomorrowKey) { formatSectionDate(tomorrowKey) }

    val eventsOnly = remember(reminders) { reminders.filter { it.category == ReminderCategory.EVENT } }
    val todayEvents = remember(eventsOnly, todayKey) { eventsOnly.filter { it.dateKey == todayKey } }
    val tomorrowEvents = remember(eventsOnly, tomorrowKey) { eventsOnly.filter { it.dateKey == tomorrowKey } }
    val futureEvents = remember(eventsOnly, tomorrowKey) {
        eventsOnly.filter { it.dateKey != null && it.dateKey > tomorrowKey }
    }
    val pastEvents = remember(eventsOnly, todayKey) {
        eventsOnly.filter { it.dateKey != null && it.dateKey < todayKey }
    }

    val allActive = remember(reminders) { reminders.filter { !it.completed } }
    val allCompleted = remember(reminders) { reminders.filter { it.completed } }
    val allTodayItems = remember(allActive, todayKey) {
        allActive.filter { it.dateKey == todayKey }.sortedWith(
            compareBy<ReminderItem> { if (it.category == ReminderCategory.EVENT) 0 else 1 }
                .thenBy { if (it.category == ReminderCategory.EVENT) it.startTimeMinutes ?: Int.MAX_VALUE else 0 }
                .thenByDescending { it.dateKey ?: "" }
                .thenByDescending { it.createdAtMs }
        )
    }
    val allTomorrowItems = remember(allActive, tomorrowKey) { allActive.filter { it.dateKey == tomorrowKey } }
    val allFutureItems = remember(allActive, tomorrowKey) {
        allActive.filter { it.dateKey != null && it.dateKey > tomorrowKey }
    }

    val filteredTasks = remember(reminders, activeTaskListFilters) {
        reminders.filter {
            it.category == ReminderCategory.TASK &&
                (activeTaskListFilters.isEmpty() || it.sourceId == null || it.sourceId in activeTaskListFilters)
        }
    }
    val activeTasks = remember(filteredTasks) { filteredTasks.filter { !it.completed } }
    val completedTasks = remember(filteredTasks) { filteredTasks.filter { it.completed } }

    NernalCard(
        modifier = modifier,
        containerColor = Color.Black,
        borderColor = if (showHeader) NernalColors.Border else Color.Transparent,
        cornerRadius = if (showHeader) 20.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showHeader) 10.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showHeader) {
                // Header: "Home Hub" + Filter Tabs
                Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Home Hub",
                    color = NernalColors.Gold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (tab in HubFilter.entries) {
                        val selected = tab == activeFilter
                        if (tab == HubFilter.EVENTS) {
                            Box {
                                Surface(
                                    color = if (selected) NernalColors.CardSubtle else NernalColors.CardInner,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) NernalColors.Gold.copy(alpha = 0.7f) else NernalColors.BorderSubtle,
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .clickable {
                                            if (!selected) {
                                                onFilterChange(HubFilter.EVENTS)
                                            }
                                            eventsMenuExpanded = !eventsMenuExpanded
                                        },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp),
                                    ) {
                                        Text(
                                            text = if (selected) "Events (${calendarViewMode.label}) ▾" else "Events ▾",
                                            color = if (selected) NernalColors.Ink else NernalColors.InkMuted,
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = eventsMenuExpanded,
                                    onDismissRequest = { eventsMenuExpanded = false },
                                    modifier = Modifier
                                        .background(NernalColors.Card)
                                        .border(1.dp, NernalColors.Border, RoundedCornerShape(12.dp)),
                                ) {
                                    for (mode in CalendarViewMode.entries) {
                                        val isCurrentMode = mode == calendarViewMode && selected
                                        val icon = when (mode) {
                                            CalendarViewMode.ITINERARY -> Icons.Rounded.ViewAgenda
                                            CalendarViewMode.THREE_DAY -> Icons.Rounded.ViewColumn
                                            CalendarViewMode.WEEK -> Icons.Rounded.CalendarViewWeek
                                            CalendarViewMode.MONTH -> Icons.Rounded.CalendarMonth
                                        }
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isCurrentMode) NernalColors.Gold else NernalColors.InkMuted,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = mode.label,
                                                    color = if (isCurrentMode) NernalColors.Gold else NernalColors.Ink,
                                                    fontWeight = if (isCurrentMode) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                onCalendarViewModeChange(mode)
                                                onFilterChange(HubFilter.EVENTS)
                                                eventsMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = if (selected) NernalColors.CardSubtle else NernalColors.CardInner,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) NernalColors.Gold.copy(alpha = 0.7f) else NernalColors.BorderSubtle,
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .clickable { onFilterChange(tab) },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                ) {
                                    Text(
                                        text = tab.label,
                                        color = if (selected) NernalColors.Ink else NernalColors.InkMuted,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    if (tabFilterableLists.size > 1) {
                        Box {
                            val checkedInTab = tabFilterableLists.count { it.id in activeTaskListFilters }
                            val hasPartialFilter = checkedInTab < tabFilterableLists.size
                            Surface(
                                color = if (hasPartialFilter) NernalColors.CardSubtle else NernalColors.CardInner,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (hasPartialFilter) NernalColors.Gold.copy(alpha = 0.85f) else NernalColors.BorderSubtle,
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .clickable { filterMenuExpanded = !filterMenuExpanded },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = "Filter Task Lists",
                                        tint = if (hasPartialFilter) NernalColors.Gold else NernalColors.InkMuted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = "Filter",
                                        color = if (hasPartialFilter) NernalColors.Gold else NernalColors.InkMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false },
                                modifier = Modifier
                                    .background(NernalColors.Card)
                                    .border(1.dp, NernalColors.Border, RoundedCornerShape(12.dp)),
                            ) {
                                tabFilterableLists.forEach { taskList ->
                                    val isChecked = taskList.id in activeTaskListFilters
                                    val canUncheck = checkedInTab > 1
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Icon(
                                                    imageVector = if (isChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    tint = if (isChecked) NernalColors.Gold else NernalColors.InkMuted,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = taskList.title,
                                                    color = if (isChecked) NernalColors.Ink else NernalColors.InkMuted,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            }
                                        },
                                        enabled = !isChecked || canUncheck,
                                        onClick = {
                                            if (!isChecked || canUncheck) {
                                                onToggleActiveTaskListFilter(taskList.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Top Right '+' Button to add task or event
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .height(48.dp)
                            .clickable { showAddDialog = true },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Task or Event",
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

            if (showAddDialog) {
                AddReminderDialog(
                    initialCategory = if (activeFilter == HubFilter.EVENTS) ReminderCategory.EVENT else ReminderCategory.TASK,
                    onDismiss = { showAddDialog = false },
                    onAdd = { t, c -> onAddReminder(t, c) },
                )
            }

            if (showHeader) {
                HorizontalDivider(color = NernalColors.BorderSubtle)
            }

            // Sectioned Items LazyColumn
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (activeFilter) {
                    HubFilter.EVENTS -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (calendarViewMode) {
                                    CalendarViewMode.MONTH -> {
                                        MonthCalendarView(
                                            reminders = reminders,
                                            availableCalendars = availableCalendars,
                                            todayKey = todayKey,
                                            onEventClick = onEventClick,
                                            onToggleReminder = onToggleReminder,
                                            isPrimaryView = false,
                                        )
                                    }
                                    CalendarViewMode.WEEK -> {
                                        WeekCalendarView(
                                            reminders = reminders,
                                            availableCalendars = availableCalendars,
                                            todayKey = todayKey,
                                            onEventClick = onEventClick,
                                            onToggleReminder = onToggleReminder,
                                            isPrimaryView = false,
                                        )
                                    }
                                    CalendarViewMode.THREE_DAY -> {
                                        ThreeDayCalendarView(
                                            reminders = reminders,
                                            availableCalendars = availableCalendars,
                                            todayKey = todayKey,
                                            onEventClick = onEventClick,
                                            onToggleReminder = onToggleReminder,
                                            isPrimaryView = false,
                                        )
                                    }
                                    CalendarViewMode.ITINERARY -> {
                                        if (eventsOnly.isEmpty()) {
                                            EmptyHubNotice("No calendar events found", "Events from your selected Google Calendars appear here")
                                        } else {
                                            LazyColumn(
                                                state = eventsListState,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .lazyListVerticalScrollbar(eventsListState),
                                                contentPadding = PaddingValues(end = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                if (todayEvents.isNotEmpty()) {
                                                    item(key = "header_ev_today") { SectionDivider("TODAY · $todayDateFormatted", todayEvents.size) }
                                                    items(todayEvents, key = { it.id }) { item ->
                                                        ReminderRow(
                                                            item = item,
                                                            todayKey = todayKey,
                                                            availableCalendars = availableCalendars,
                                                            onToggle = { onEventClick(item) },
                                                            onDelete = { onDeleteReminder(item.id) },
                                                            onEventClick = { onEventClick(item) },
                                                        )
                                                    }
                                                }

                                                if (tomorrowEvents.isNotEmpty()) {
                                                    item(key = "header_ev_tomorrow") { SectionDivider("TOMORROW · $tomorrowDateFormatted", tomorrowEvents.size) }
                                                    items(tomorrowEvents, key = { it.id }) { item ->
                                                        ReminderRow(
                                                            item = item,
                                                            todayKey = todayKey,
                                                            availableCalendars = availableCalendars,
                                                            onToggle = { onEventClick(item) },
                                                            onDelete = { onDeleteReminder(item.id) },
                                                            onEventClick = { onEventClick(item) },
                                                        )
                                                    }
                                                }

                                                if (futureEvents.isNotEmpty()) {
                                                    item(key = "header_ev_future") { SectionDivider("UPCOMING & FUTURE", futureEvents.size) }
                                                    items(futureEvents, key = { it.id }) { item ->
                                                        ReminderRow(
                                                            item = item,
                                                            todayKey = todayKey,
                                                            availableCalendars = availableCalendars,
                                                            onToggle = { onEventClick(item) },
                                                            onDelete = { onDeleteReminder(item.id) },
                                                            onEventClick = { onEventClick(item) },
                                                        )
                                                    }
                                                }

                                                if (pastEvents.isNotEmpty()) {
                                                    item(key = "header_ev_past") { SectionDivider("PAST / RECENT", pastEvents.size) }
                                                    items(pastEvents, key = { it.id }) { item ->
                                                        ReminderRow(
                                                            item = item,
                                                            todayKey = todayKey,
                                                            availableCalendars = availableCalendars,
                                                            onToggle = { onEventClick(item) },
                                                            onDelete = { onDeleteReminder(item.id) },
                                                            onEventClick = { onEventClick(item) },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    HubFilter.ALL -> {
                        if (reminders.isEmpty()) {
                            EmptyHubNotice("No reminders or list items yet", "Add a reminder, appointment, or grocery item below")
                        } else {
                            LazyColumn(
                                state = allListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .lazyListVerticalScrollbar(allListState),
                                contentPadding = PaddingValues(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                if (allTodayItems.isNotEmpty()) {
                                    item(key = "header_all_today") { SectionDivider("TODAY · $todayDateFormatted", allTodayItems.size) }
                                    items(allTodayItems, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            availableCalendars = availableCalendars,
                                            onToggle = {
                                                if (item.category == ReminderCategory.EVENT) onEventClick(item)
                                                else onToggleReminder(item.id)
                                            },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { onEventClick(item) },
                                        )
                                    }
                                }

                                if (allTomorrowItems.isNotEmpty()) {
                                    item(key = "header_all_tomorrow") { SectionDivider("TOMORROW · $tomorrowDateFormatted", allTomorrowItems.size) }
                                    items(allTomorrowItems, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            availableCalendars = availableCalendars,
                                            onToggle = {
                                                if (item.category == ReminderCategory.EVENT) onEventClick(item)
                                                else onToggleReminder(item.id)
                                            },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { onEventClick(item) },
                                        )
                                    }
                                }

                                if (allFutureItems.isNotEmpty()) {
                                    item(key = "header_all_future") { SectionDivider("UPCOMING & FUTURE", allFutureItems.size) }
                                    items(allFutureItems, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            availableCalendars = availableCalendars,
                                            onToggle = {
                                                if (item.category == ReminderCategory.EVENT) onEventClick(item)
                                                else onToggleReminder(item.id)
                                            },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { onEventClick(item) },
                                        )
                                    }
                                }

                                if (allCompleted.isNotEmpty()) {
                                    item(key = "header_all_completed") { SectionDivider("COMPLETED", allCompleted.size) }
                                    items(allCompleted, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            onToggle = { onToggleReminder(item.id) },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { onEventClick(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HubFilter.TASKS -> {
                        if (filteredTasks.isEmpty()) {
                            EmptyHubNotice("No tasks yet", "Add a task or to-do item below")
                        } else {
                            LazyColumn(
                                state = tasksListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .lazyListVerticalScrollbar(tasksListState),
                                contentPadding = PaddingValues(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                if (activeTasks.isNotEmpty()) {
                                    item(key = "header_tasks_active") { SectionDivider("TO-DO TASKS", activeTasks.size) }
                                    items(activeTasks, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            onToggle = { onToggleReminder(item.id) },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { },
                                        )
                                    }
                                }
                                if (completedTasks.isNotEmpty()) {
                                    item(key = "header_tasks_completed") { SectionDivider("COMPLETED", completedTasks.size) }
                                    items(completedTasks, key = { it.id }) { item ->
                                        ReminderRow(
                                            item = item,
                                            todayKey = todayKey,
                                            onToggle = { onToggleReminder(item.id) },
                                            onDelete = { onDeleteReminder(item.id) },
                                            onEventClick = { },
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
}

@Composable
private fun SectionDivider(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = NernalColors.Gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        if (count != null && count > 0) {
            Text(
                text = "($count)",
                color = NernalColors.InkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(NernalColors.BorderSubtle),
        )
    }
}

@Composable
private fun EmptyHubNotice(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = NernalColors.InkMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = NernalColors.InkMuted.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ReminderRow(
    item: ReminderItem,
    todayKey: String,
    availableCalendars: List<GoogleCalendarInfo> = emptyList(),
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEventClick: () -> Unit,
) {
    val isEvent = item.category == ReminderCategory.EVENT
    val accent = if (isEvent) eventColor(item, availableCalendars) else categoryColor(item.category)

    Surface(
        color = NernalColors.CardInner,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NernalColors.BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isEvent) onEventClick()
                else onToggle()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Leading target: Calendar event icon or 48dp checkbox touch area
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (!isEvent) Modifier.clickable(onClick = onToggle) else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isEvent) {
                    Surface(
                        color = accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Event",
                                tint = accent,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                } else {
                    Surface(
                        color = if (item.completed) NernalColors.Emerald else Color.Transparent,
                        shape = CircleShape,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (item.completed) NernalColors.Emerald else NernalColors.InkMuted,
                        ),
                        modifier = Modifier.size(22.dp),
                    ) {
                        if (item.completed) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Completed",
                                    tint = NernalColors.Bg,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Title
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.text,
                    color = if (item.completed) NernalColors.InkMuted else NernalColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = if (isEvent) FontWeight.Bold else FontWeight.SemiBold,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Time / Date Chip
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            ) {
                val dateChip = formatShortDate(item.dateKey, todayKey)
                val tagText = buildString {
                    if (!item.time.isNullOrBlank()) append("${item.time} · ")
                    if (dateChip.isNotEmpty()) append(dateChip) else append(item.category.label)
                }
                Text(
                    text = tagText,
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete item",
                    tint = NernalColors.InkMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun WeatherAlertsDialog(
    alerts: List<WeatherAlert>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NernalColors.Card,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = NernalColors.Rose)
                Text(
                    text = "Active Weather Alerts (${alerts.size})",
                    color = NernalColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (alert in alerts) {
                    Surface(
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Rose.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${alert.event}${alert.severity.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
                                color = NernalColors.Rose,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (alert.headline.isNotBlank()) {
                                Text(
                                    text = alert.headline,
                                    color = NernalColors.Gold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (alert.description.isNotBlank()) {
                                Text(
                                    text = alert.description,
                                    color = NernalColors.Ink,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Close", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
    )
}

@Composable
fun EventDetailsDialog(
    item: ReminderItem,
    availableCalendars: List<GoogleCalendarInfo> = emptyList(),
    onDismiss: () -> Unit,
) {
    val friendlyCalendar = remember(item.sourceId, availableCalendars) {
        if (item.sourceId.isNullOrBlank()) null
        else availableCalendars.find { it.id == item.sourceId }?.summary ?: item.sourceId
    }

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
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = NernalColors.Accent,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = item.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NernalColors.Ink,
                )
            }
        },
        text = {
            val eventScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(eventScrollState)
                    .verticalScrollbar(eventScrollState)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Time & Date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = NernalColors.Gold,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = buildString {
                            if (!item.time.isNullOrBlank()) append("${item.time} · ")
                            if (!item.dateKey.isNullOrBlank()) append(formatSectionDate(item.dateKey))
                        }.ifBlank { "All Day Event" },
                        color = NernalColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!friendlyCalendar.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = NernalColors.Gold,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Calendar: $friendlyCalendar",
                            color = NernalColors.InkMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (!item.notes.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Notes & Details:",
                            color = NernalColors.InkMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            color = NernalColors.CardInner,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NernalColors.BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = item.notes,
                                color = NernalColors.Ink,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NernalColors.Gold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Close", color = NernalColors.Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
    )
}

@Composable
fun AddReminderDialog(
    initialCategory: ReminderCategory = ReminderCategory.TASK,
    onDismiss: () -> Unit,
    onAdd: (text: String, category: ReminderCategory) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NernalColors.Card,
        titleContentColor = NernalColors.Ink,
        textContentColor = NernalColors.Ink,
        title = {
            Text(
                text = "New Entry",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NernalColors.Gold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Category Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val categories = listOf(ReminderCategory.TASK, ReminderCategory.EVENT)
                    for (cat in categories) {
                        val isSel = cat == category
                        val color = if (cat == ReminderCategory.EVENT) NernalColors.Accent else NernalColors.Amber
                        Surface(
                            color = if (isSel) color.copy(alpha = 0.22f) else NernalColors.CardInner,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSel) color else NernalColors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { category = cat },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = if (cat == ReminderCategory.EVENT) Icons.Rounded.Event else Icons.Rounded.Checklist,
                                    contentDescription = null,
                                    tint = if (isSel) color else NernalColors.InkMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.label,
                                    color = if (isSel) NernalColors.Ink else NernalColors.InkMuted,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }

                // Text field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = if (category == ReminderCategory.EVENT) "e.g., Dentist appointment at 2 PM" else "e.g., Feed Riker",
                            color = NernalColors.InkMuted,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NernalColors.Gold,
                        unfocusedBorderColor = NernalColors.Border,
                        focusedTextColor = NernalColors.Ink,
                        unfocusedTextColor = NernalColors.Ink,
                        cursorColor = NernalColors.Gold,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onAdd(trimmed, category)
                            onDismiss()
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onAdd(trimmed, category)
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NernalColors.Gold,
                    contentColor = NernalColors.Bg,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Add ${category.label}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NernalColors.Border),
                modifier = Modifier.height(48.dp),
            ) {
                Text("Cancel", color = NernalColors.InkMuted, fontSize = 15.sp)
            }
        },
    )
}

private fun formatSectionDate(dateKey: String): String = runCatching {
    LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US))
}.getOrDefault(dateKey)

private fun formatShortDate(dateKey: String?, todayKey: String): String {
    if (dateKey.isNullOrBlank()) return ""
    if (dateKey == todayKey) return "Today"
    return runCatching {
        LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }.getOrDefault(dateKey)
}

private fun categoryColor(category: ReminderCategory): Color = when (category) {
    ReminderCategory.EVENT -> NernalColors.Accent
    ReminderCategory.TASK -> NernalColors.Amber
}

private fun weatherIconAndTint(code: Int, isDay: Boolean): Pair<ImageVector, Color> =
    when (wmoKind(code)) {
        WeatherConditionKind.CLEAR ->
            if (isDay) Icons.Rounded.WbSunny to NernalColors.Amber
            else Icons.Rounded.DarkMode to NernalColors.Gold
        WeatherConditionKind.PARTLY_CLOUDY -> Icons.Rounded.WbCloudy to NernalColors.Gold
        WeatherConditionKind.CLOUDY -> Icons.Rounded.Cloud to NernalColors.InkMuted
        WeatherConditionKind.FOG -> Icons.Rounded.FilterDrama to NernalColors.InkMuted
        WeatherConditionKind.RAIN -> Icons.Rounded.Grain to NernalColors.Accent
        WeatherConditionKind.SNOW -> Icons.Rounded.AcUnit to Color(0xFFBAE6FD)
        WeatherConditionKind.STORM -> Icons.Rounded.Thunderstorm to NernalColors.Amber
    }

private fun precipIconAndTint(code: Int): Pair<ImageVector, Color> =
    when (wmoKind(code)) {
        WeatherConditionKind.SNOW -> Icons.Rounded.AcUnit to Color(0xFFBAE6FD)
        WeatherConditionKind.STORM -> Icons.Rounded.Thunderstorm to NernalColors.Amber
        else -> Icons.Rounded.WaterDrop to NernalColors.Accent
    }

@Composable
fun MonthCalendarView(
    reminders: List<ReminderItem>,
    availableCalendars: List<GoogleCalendarInfo>,
    todayKey: String,
    onEventClick: (ReminderItem) -> Unit,
    onToggleReminder: (String) -> Unit = {},
    isPrimaryView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val initialDate = remember(todayKey) {
        runCatching { LocalDate.parse(todayKey) }.getOrDefault(LocalDate.now())
    }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val eventsByDate = remember(reminders) {
        reminders.filter { it.category == ReminderCategory.EVENT && it.dateKey != null }
            .groupBy { it.dateKey!! }
    }
    val tasksByDate = remember(reminders) {
        reminders.filter { it.category == ReminderCategory.TASK && it.dateKey != null }
            .groupBy { it.dateKey!! }
    }

    val selectedDateEvents = remember(reminders, selectedDate) {
        val dateKey = selectedDate.toString()
        reminders.filter { it.category == ReminderCategory.EVENT && it.dateKey == dateKey }
            .sortedWith(compareBy({ if (it.isAllDay || it.startTimeMinutes == null) -1 else it.startTimeMinutes!! }, { it.createdAtMs }))
    }
    val selectedDateTasks = remember(reminders, selectedDate) {
        val dateKey = selectedDate.toString()
        reminders.filter { it.category == ReminderCategory.TASK && it.dateKey == dateKey }
            .sortedWith(compareBy({ it.completed }, { it.startTimeMinutes ?: Int.MAX_VALUE }, { it.createdAtMs }))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous Month", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }

                Text(
                    text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                    color = NernalColors.Gold,
                    fontSize = if (isPrimaryView) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronRight, "Next Month", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }
            }

            // Jump to Today button if not viewing current month
            val isCurrentMonth = currentYearMonth == YearMonth.from(initialDate)
            if (!isCurrentMonth) {
                Surface(
                    color = NernalColors.CardSubtle,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier
                        .clickable {
                            currentYearMonth = YearMonth.from(initialDate)
                            selectedDate = initialDate
                        },
                ) {
                    Text(
                        text = "Today",
                        color = NernalColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Day of Week Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            for (day in weekdays) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day,
                        color = NernalColors.InkMuted,
                        fontSize = if (isPrimaryView) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Days Grid (6 rows of 7 days)
        val firstOfMonth = currentYearMonth.atDay(1)
        val daysInMonth = currentYearMonth.lengthOfMonth()
        val firstDayOfWeek = firstOfMonth.dayOfWeek.value % 7
        val prevMonth = currentYearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isPrimaryView) Modifier.weight(1f) else Modifier),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (weekIdx in 0 until 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isPrimaryView) Modifier.weight(1f) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (dayOfWeekIdx in 0 until 7) {
                        val cellIdx = weekIdx * 7 + dayOfWeekIdx
                        val isPrevMonth = cellIdx < firstDayOfWeek
                        val isNextMonth = cellIdx >= firstDayOfWeek + daysInMonth
                        val isCurrMonth = !isPrevMonth && !isNextMonth

                        val (displayDay, date) = when {
                            isPrevMonth -> {
                                val d = daysInPrevMonth - (firstDayOfWeek - cellIdx - 1)
                                d to prevMonth.atDay(d)
                            }
                            isNextMonth -> {
                                val d = cellIdx - (firstDayOfWeek + daysInMonth) + 1
                                d to currentYearMonth.plusMonths(1).atDay(d)
                            }
                            else -> {
                                val d = cellIdx - firstDayOfWeek + 1
                                d to currentYearMonth.atDay(d)
                            }
                        }

                        val dateKey = date.toString()
                        val isToday = dateKey == todayKey
                        val isSelected = date == selectedDate
                        val dayEvents = eventsByDate[dateKey].orEmpty().sortedWith(
                            compareBy(
                                { if (it.isAllDay || it.startTimeMinutes == null) -1 else it.startTimeMinutes!! },
                                { it.createdAtMs }
                            )
                        )
                        val dayTasks = tasksByDate[dateKey].orEmpty()
                        val uncompletedTasks = dayTasks.filter { !it.completed }
                        val completedTasks = dayTasks.filter { it.completed }

                        Surface(
                            color = when {
                                isSelected -> NernalColors.CardSubtle
                                else -> Color.Black
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.5.dp,
                                color = when {
                                    isSelected -> NernalColors.Gold
                                    isToday -> NernalColors.Accent
                                    else -> NernalColors.BorderSubtle.copy(alpha = 0.5f)
                                },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .then(if (isPrimaryView) Modifier.fillMaxHeight() else Modifier.height(44.dp))
                                .clickable {
                                    selectedDate = date
                                    if (!isCurrMonth) {
                                        currentYearMonth = YearMonth.from(date)
                                    }
                                },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (isPrimaryView) Arrangement.Top else Arrangement.Center,
                            ) {
                                // Day Number
                                Box(
                                    modifier = Modifier
                                        .size(if (isPrimaryView) 24.dp else 18.dp)
                                        .then(
                                            if (isToday) Modifier
                                                .clip(CircleShape)
                                                .background(NernalColors.Accent)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = displayDay.toString(),
                                        color = when {
                                            isToday -> Color.Black
                                            isCurrMonth -> NernalColors.Ink
                                            else -> NernalColors.InkMuted.copy(alpha = 0.35f)
                                        },
                                        fontSize = if (isPrimaryView) 14.sp else 11.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }

                                if (isPrimaryView) {
                                    // In wide primary view: render event chips with calendar colors
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                    ) {
                                        val displayEvents = dayEvents.take(2)
                                        for (ev in displayEvents) {
                                            val color = eventColor(ev, availableCalendars)
                                            val isAllDayEv = ev.isAllDay || ev.startTimeMinutes == null
                                            Surface(
                                                color = color.copy(alpha = if (isAllDayEv) 0.30f else 0.22f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, color.copy(alpha = 0.7f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onEventClick(ev) },
                                            ) {
                                                Text(
                                                    text = ev.text,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isAllDayEv) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        if (dayEvents.size > 2) {
                                            Text(
                                                text = "+${dayEvents.size - 2} more",
                                                color = NernalColors.InkMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 2.dp),
                                            )
                                        }

                                        // Task Indicator Row in Primary Month View
                                        if (dayTasks.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 1.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (uncompletedTasks.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    ) {
                                                        Surface(
                                                            color = Color.Transparent,
                                                            shape = CircleShape,
                                                            border = BorderStroke(1.2.dp, NernalColors.Gold),
                                                            modifier = Modifier.size(7.dp),
                                                        ) {}
                                                        Text(
                                                            text = "${uncompletedTasks.size}",
                                                            color = NernalColors.Gold,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                                if (completedTasks.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    ) {
                                                        Surface(
                                                            color = NernalColors.Emerald,
                                                            shape = CircleShape,
                                                            modifier = Modifier.size(7.dp),
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Check,
                                                                    contentDescription = "Done",
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(5.dp),
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${completedTasks.size}",
                                                            color = NernalColors.Emerald,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // In compact split view: render colored event dots + task indicators
                                    if (dayEvents.isNotEmpty() || dayTasks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            for (ev in dayEvents.take(3)) {
                                                val color = eventColor(ev, availableCalendars)
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                            }
                                            if (uncompletedTasks.isNotEmpty()) {
                                                Surface(
                                                    color = Color.Transparent,
                                                    shape = CircleShape,
                                                    border = BorderStroke(1.2.dp, NernalColors.Gold),
                                                    modifier = Modifier.size(5.dp),
                                                ) {}
                                            }
                                            if (completedTasks.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(NernalColors.Emerald)
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
        }

        // In compact mode: show selected day's events & tasks below the calendar
        if (!isPrimaryView) {
            HorizontalDivider(color = NernalColors.BorderSubtle)
            val selectedDateFormatted = remember(selectedDate) {
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
            }
            Text(
                text = "Events & Tasks · $selectedDateFormatted",
                color = NernalColors.Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            if (selectedDateEvents.isEmpty() && selectedDateTasks.isEmpty()) {
                Text(
                    text = "No events or tasks scheduled for this day",
                    color = NernalColors.InkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    items(selectedDateEvents, key = { it.id }) { item ->
                        ReminderRow(
                            item = item,
                            todayKey = todayKey,
                            availableCalendars = availableCalendars,
                            onToggle = { onEventClick(item) },
                            onDelete = {},
                            onEventClick = { onEventClick(item) },
                        )
                    }
                    if (selectedDateTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tasks for this day",
                                color = NernalColors.Gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        items(selectedDateTasks, key = { it.id }) { item ->
                            ReminderRow(
                                item = item,
                                todayKey = todayKey,
                                availableCalendars = availableCalendars,
                                onToggle = { onToggleReminder(item.id) },
                                onDelete = {},
                                onEventClick = { onToggleReminder(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeDayCalendarView(
    reminders: List<ReminderItem>,
    availableCalendars: List<GoogleCalendarInfo>,
    todayKey: String,
    onEventClick: (ReminderItem) -> Unit,
    onToggleReminder: (String) -> Unit = {},
    isPrimaryView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val initialDate = remember(todayKey) {
        runCatching { LocalDate.parse(todayKey) }.getOrDefault(LocalDate.now())
    }
    var startDate by remember { mutableStateOf(initialDate) }

    val days = remember(startDate) {
        (0..2).map { startDate.plusDays(it.toLong()) }
    }
    val startKey = remember(days) { days.first().toString() }
    val endKey = remember(days) { days.last().toString() }

    val rangeItems = remember(reminders, startKey, endKey) {
        reminders.filter {
            (it.category == ReminderCategory.EVENT || it.category == ReminderCategory.TASK) &&
                it.dateKey != null &&
                it.dateKey >= startKey &&
                it.dateKey <= endKey
        }
    }
    val itemsByDate = remember(rangeItems) {
        rangeItems.groupBy { it.dateKey ?: "" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { startDate = startDate.minusDays(3) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous 3 Days", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }

                val rangeLabel = remember(days) {
                    val startFmt = days.first().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                    val endFmt = days.last().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                    "$startFmt – $endFmt"
                }
                Text(
                    text = rangeLabel,
                    color = NernalColors.Gold,
                    fontSize = if (isPrimaryView) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(
                    onClick = { startDate = startDate.plusDays(3) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronRight, "Next 3 Days", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }
            }

            // Jump to Today button if navigated away
            if (startDate != initialDate) {
                Surface(
                    color = NernalColors.CardSubtle,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier.clickable { startDate = initialDate },
                ) {
                    Text(
                        text = "Today",
                        color = NernalColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // 3 Day Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (date in days) {
                val dateKey = date.toString()
                val isToday = dateKey == todayKey
                val isTomorrow = date == initialDate.plusDays(1)
                val dayItems = itemsByDate[dateKey].orEmpty()
                val dayEvents = dayItems.filter { it.category == ReminderCategory.EVENT }.sortedWith(
                    compareBy(
                        { if (it.isAllDay || it.startTimeMinutes == null) -1 else it.startTimeMinutes!! },
                        { it.createdAtMs }
                    )
                )
                val dayTasks = dayItems.filter { it.category == ReminderCategory.TASK }.sortedWith(
                    compareBy(
                        { it.completed },
                        { it.startTimeMinutes ?: Int.MAX_VALUE },
                        { it.createdAtMs }
                    )
                )

                val dayBadge = when {
                    isToday -> "TODAY"
                    isTomorrow -> "TOMORROW"
                    else -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.US)).uppercase(Locale.US)
                }
                val daySub = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) NernalColors.Accent else NernalColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Header
                        Surface(
                            color = if (isToday) NernalColors.Accent.copy(alpha = 0.2f) else NernalColors.CardSubtle,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = dayBadge,
                                    color = if (isToday) NernalColors.Accent else NernalColors.InkMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                )
                                Text(
                                    text = daySub,
                                    color = if (isToday) NernalColors.Accent else NernalColors.Ink,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // Content
                        if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No events or tasks",
                                    color = NernalColors.InkMuted.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                // Events
                                items(dayEvents, key = { it.id }) { ev ->
                                    val color = eventColor(ev, availableCalendars)
                                    val isAllDayEv = ev.isAllDay || ev.startTimeMinutes == null
                                    Surface(
                                        color = color.copy(alpha = if (isAllDayEv) 0.30f else 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, color.copy(alpha = if (isAllDayEv) 0.85f else 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEventClick(ev) },
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (isAllDayEv) {
                                                Text(
                                                    text = "ALL DAY",
                                                    color = color,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                )
                                            } else if (!ev.time.isNullOrBlank()) {
                                                Text(
                                                    text = ev.time,
                                                    color = color,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Text(
                                                text = ev.text,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }

                                // Tasks Section
                                if (dayTasks.isNotEmpty()) {
                                    if (dayEvents.isNotEmpty()) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = NernalColors.BorderSubtle)
                                                Text(
                                                    text = "TASKS" + (if (dayTasks.any { !it.completed }) " (${dayTasks.count { !it.completed }})" else ""),
                                                    color = NernalColors.Gold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                )
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = NernalColors.BorderSubtle)
                                            }
                                        }
                                    }

                                    items(dayTasks, key = { it.id }) { task ->
                                        Surface(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (task.completed) NernalColors.BorderSubtle else NernalColors.Gold.copy(alpha = 0.55f),
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onToggleReminder(task.id) },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Surface(
                                                    color = if (task.completed) NernalColors.Emerald else Color.Transparent,
                                                    shape = CircleShape,
                                                    border = BorderStroke(
                                                        width = 1.5.dp,
                                                        color = if (task.completed) NernalColors.Emerald else NernalColors.Gold,
                                                    ),
                                                    modifier = Modifier.size(20.dp),
                                                ) {
                                                    if (task.completed) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = "Completed",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(13.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                                ) {
                                                    if (!task.time.isNullOrBlank()) {
                                                        Text(
                                                            text = task.time,
                                                            color = if (task.completed) NernalColors.InkMuted else NernalColors.Gold,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                    Text(
                                                        text = task.text,
                                                        color = if (task.completed) NernalColors.InkMuted else Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
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
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    reminders: List<ReminderItem>,
    availableCalendars: List<GoogleCalendarInfo>,
    todayKey: String,
    onEventClick: (ReminderItem) -> Unit,
    onToggleReminder: (String) -> Unit = {},
    isPrimaryView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val initialDate = remember(todayKey) {
        runCatching { LocalDate.parse(todayKey) }.getOrDefault(LocalDate.now())
    }
    val currentSunday = remember(initialDate) {
        val daysSinceSunday = initialDate.dayOfWeek.value % 7
        initialDate.minusDays(daysSinceSunday.toLong())
    }
    var weekStart by remember { mutableStateOf(currentSunday) }

    val weekDays = remember(weekStart) {
        (0..6).map { weekStart.plusDays(it.toLong()) }
    }

    val weekEnd = remember(weekDays) { weekDays.last() }

    val weekItems = remember(reminders, weekStart, weekEnd) {
        val startKey = weekStart.toString()
        val endKey = weekEnd.toString()
        reminders.filter {
            (it.category == ReminderCategory.EVENT || it.category == ReminderCategory.TASK) &&
                it.dateKey != null &&
                it.dateKey >= startKey &&
                it.dateKey <= endKey
        }
    }
    val itemsByDate = remember(weekItems) {
        weekItems.groupBy { it.dateKey ?: "" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Week Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { weekStart = weekStart.minusWeeks(1) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous Week", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }

                val rangeLabel = remember(weekStart, weekEnd) {
                    val startFmt = weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                    val endFmt = weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                    "$startFmt – $endFmt"
                }
                Text(
                    text = rangeLabel,
                    color = NernalColors.Gold,
                    fontSize = if (isPrimaryView) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(
                    onClick = { weekStart = weekStart.plusWeeks(1) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Rounded.ChevronRight, "Next Week", tint = NernalColors.Ink, modifier = Modifier.size(26.dp))
                }
            }

            // Jump to This Week button
            if (weekStart != currentSunday) {
                Surface(
                    color = NernalColors.CardSubtle,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NernalColors.Border),
                    modifier = Modifier
                        .clickable { weekStart = currentSunday },
                ) {
                    Text(
                        text = "This Week",
                        color = NernalColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // 7 Day Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (date in weekDays) {
                val dateKey = date.toString()
                val isToday = dateKey == todayKey
                val dayItems = itemsByDate[dateKey].orEmpty()
                val dayEvents = dayItems.filter { it.category == ReminderCategory.EVENT }.sortedWith(
                    compareBy(
                        { if (it.isAllDay || it.startTimeMinutes == null) -1 else it.startTimeMinutes!! },
                        { it.createdAtMs }
                    )
                )
                val dayTasks = dayItems.filter { it.category == ReminderCategory.TASK }.sortedWith(
                    compareBy(
                        { it.completed },
                        { it.startTimeMinutes ?: Int.MAX_VALUE },
                        { it.createdAtMs }
                    )
                )
                val dayName = date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase(Locale.US)
                val dayNum = date.dayOfMonth.toString()

                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = if (isToday) 1.5.dp else 1.dp,
                        color = if (isToday) NernalColors.Accent else NernalColors.BorderSubtle,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Column Header
                        Surface(
                            color = if (isToday) NernalColors.Accent.copy(alpha = 0.2f) else NernalColors.CardSubtle,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = dayName,
                                    color = if (isToday) NernalColors.Accent else NernalColors.InkMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = dayNum,
                                    color = if (isToday) NernalColors.Accent else NernalColors.Ink,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // Events & Tasks in this Day Column
                        if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "—",
                                    color = NernalColors.InkMuted.copy(alpha = 0.3f),
                                    fontSize = 15.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(dayEvents, key = { it.id }) { ev ->
                                    val color = eventColor(ev, availableCalendars)
                                    val isAllDayEv = ev.isAllDay || ev.startTimeMinutes == null
                                    Surface(
                                        color = color.copy(alpha = if (isAllDayEv) 0.30f else 0.18f),
                                        shape = RoundedCornerShape(7.dp),
                                        border = BorderStroke(1.dp, color.copy(alpha = if (isAllDayEv) 0.85f else 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEventClick(ev) },
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            if (isAllDayEv) {
                                                Text(
                                                    text = "ALL DAY",
                                                    color = color,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                )
                                            } else if (!ev.time.isNullOrBlank()) {
                                                Text(
                                                    text = ev.time,
                                                    color = color,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Text(
                                                text = ev.text,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }

                                if (dayTasks.isNotEmpty()) {
                                    if (dayEvents.isNotEmpty()) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = NernalColors.BorderSubtle)
                                                Text(
                                                    text = "TASKS" + (if (dayTasks.any { !it.completed }) " (${dayTasks.count { !it.completed }})" else ""),
                                                    color = NernalColors.Gold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                )
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = NernalColors.BorderSubtle)
                                            }
                                        }
                                    }

                                    items(dayTasks, key = { it.id }) { task ->
                                        Surface(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(7.dp),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (task.completed) NernalColors.BorderSubtle else NernalColors.Gold.copy(alpha = 0.55f),
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onToggleReminder(task.id) },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Surface(
                                                    color = if (task.completed) NernalColors.Emerald else Color.Transparent,
                                                    shape = CircleShape,
                                                    border = BorderStroke(
                                                        width = 1.5.dp,
                                                        color = if (task.completed) NernalColors.Emerald else NernalColors.Gold,
                                                    ),
                                                    modifier = Modifier.size(18.dp),
                                                ) {
                                                    if (task.completed) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = "Completed",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(12.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                                ) {
                                                    if (!task.time.isNullOrBlank()) {
                                                        Text(
                                                            text = task.time,
                                                            color = if (task.completed) NernalColors.InkMuted else NernalColors.Gold,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                    Text(
                                                        text = task.text,
                                                        color = if (task.completed) NernalColors.InkMuted else Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
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
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarPrimaryWing(
    state: NernalUiState,
    onRefreshWeather: () -> Unit,
    onOpenRadar: () -> Unit,
    onFilterChange: (HubFilter) -> Unit,
    onToggleActiveTaskListFilter: (String) -> Unit = {},
    onAddReminder: (String, ReminderCategory) -> Unit,
    onToggleReminder: (String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onEventClick: (ReminderItem) -> Unit,
    onCalendarViewModeChange: (CalendarViewMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAccount: () -> Unit,
    onDimScreen: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onToggleCalendarPrimary: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var calMenuExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top Unified Bar across full width
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // LEFT ROW: Time | Date · Location | Weather | Radar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    // Time (larger: 44sp) + AM/PM
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = state.clockTime.ifEmpty { "--:--" },
                            color = NernalColors.Ink,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                        Text(
                            text = state.clockAmPm,
                            color = NernalColors.WarmAmber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(NernalColors.BorderSubtle),
                    )

                    // Date · Location inline (never on separate lines)
                    val dateAndLoc = remember(state.clockDate, state.userLocation.name) {
                        val loc = state.userLocation.name.trim()
                        if (loc.isNotBlank()) "${state.clockDate} · $loc" else state.clockDate
                    }
                    Text(
                        text = dateAndLoc,
                        color = NernalColors.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(NernalColors.BorderSubtle),
                    )

                    // Weather plate: looks like UnifiedHeaderCard (Icon, Temp, Desc + H/L)
                    val w = state.weather
                    if (w != null) {
                        val (icon, tint) = weatherIconAndTint(w.weatherCode, w.isDay)
                        val todayForecast = w.daily.firstOrNull()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = w.description,
                                tint = tint,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                text = "${w.currentTempF}°",
                                color = NernalColors.Ink,
                                fontSize = 34.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = w.description,
                                    color = NernalColors.Ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                if (todayForecast != null) {
                                    Text(
                                        text = "H:${todayForecast.maxTempF}° L:${todayForecast.minTempF}°",
                                        color = NernalColors.InkMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // Radar button
                    Surface(
                        color = NernalColors.CardSubtle,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable(onClick = onOpenRadar),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Radar",
                                tint = NernalColors.Accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Radar",
                                color = NernalColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // RIGHT ROW: Show Photos, Wi-Fi, Dim, Help, Account, SETTINGS (all the way to the right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onToggleCalendarPrimary != null) {
                        Surface(
                            onClick = {
                                android.util.Log.d("NernalClick", "Show Photos clicked!")
                                onToggleCalendarPrimary(false)
                            },
                            color = NernalColors.Card,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.6f)),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = "Show Photo Carousel",
                                    tint = NernalColors.Gold,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Show Photos",
                                    color = NernalColors.Gold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenWifiSettings,
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Wifi, "Wi-Fi Settings", tint = NernalColors.Accent, modifier = Modifier.size(22.dp))
                        }
                    }

                    Surface(
                        onClick = onDimScreen,
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Bedtime, "Sleep / Dim Display", tint = NernalColors.Gold, modifier = Modifier.size(22.dp))
                        }
                    }

                    Surface(
                        onClick = onOpenHelp,
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("?", color = NernalColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Account icon with sync dot
                    val dotColor = when {
                        state.authState == GoogleAuthState.SIGNED_IN && state.googleSyncing -> NernalColors.WarmAmber
                        state.authState == GoogleAuthState.SIGNED_IN -> NernalColors.Emerald
                        state.authState == GoogleAuthState.SIGNING_IN -> NernalColors.Accent
                        state.authState == GoogleAuthState.ERROR -> NernalColors.Rose
                        else -> NernalColors.Amber
                    }
                    Surface(
                        onClick = onOpenAccount,
                        color = if (state.authState == GoogleAuthState.NOT_SIGNED_IN) Color(0xFF282213) else NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, "Account", tint = NernalColors.Ink, modifier = Modifier.size(22.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                            )
                        }
                    }

                    // Settings: All the way to the right
                    Surface(
                        onClick = onOpenSettings,
                        color = NernalColors.Card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NernalColors.Border),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Settings, "Frame Settings", tint = NernalColors.Gold, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // Main Stage Container
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NernalColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header with Primary Navigation Tabs (Calendar / Tasks / All)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Left: Filter tabs
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (tab in HubFilter.entries) {
                            val selected = tab == state.hubFilter
                            if (tab == HubFilter.EVENTS) {
                                Box {
                                    Surface(
                                        onClick = {
                                            if (!selected) {
                                                onFilterChange(HubFilter.EVENTS)
                                            }
                                            calMenuExpanded = !calMenuExpanded
                                        },
                                        color = if (selected) NernalColors.CardSubtle else Color.Black,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (selected) NernalColors.Gold.copy(alpha = 0.8f) else NernalColors.BorderSubtle,
                                        ),
                                        modifier = Modifier.height(44.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                        ) {
                                            Text(
                                                text = if (selected) "Calendar (${state.calendarViewMode.label}) ▾" else "Calendar ▾",
                                                color = if (selected) NernalColors.Ink else NernalColors.InkMuted,
                                                fontSize = 15.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = calMenuExpanded,
                                        onDismissRequest = { calMenuExpanded = false },
                                        modifier = Modifier
                                            .background(NernalColors.Card)
                                            .border(1.dp, NernalColors.Border, RoundedCornerShape(12.dp)),
                                    ) {
                                        for (mode in CalendarViewMode.entries) {
                                            val isCurrentMode = mode == state.calendarViewMode && selected
                                            val icon = when (mode) {
                                                CalendarViewMode.ITINERARY -> Icons.Rounded.ViewAgenda
                                                CalendarViewMode.THREE_DAY -> Icons.Rounded.ViewColumn
                                                CalendarViewMode.WEEK -> Icons.Rounded.CalendarViewWeek
                                                CalendarViewMode.MONTH -> Icons.Rounded.CalendarMonth
                                            }
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isCurrentMode) NernalColors.Gold else NernalColors.InkMuted,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        text = mode.label,
                                                        color = if (isCurrentMode) NernalColors.Gold else NernalColors.Ink,
                                                        fontWeight = if (isCurrentMode) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                },
                                                onClick = {
                                                    onCalendarViewModeChange(mode)
                                                    onFilterChange(HubFilter.EVENTS)
                                                    calMenuExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = { onFilterChange(tab) },
                                    color = if (selected) NernalColors.CardSubtle else Color.Black,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) NernalColors.Gold.copy(alpha = 0.8f) else NernalColors.BorderSubtle,
                                    ),
                                    modifier = Modifier.height(44.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                    ) {
                                        Text(
                                            text = tab.label,
                                            color = if (selected) NernalColors.Ink else NernalColors.InkMuted,
                                            fontSize = 15.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right: Add button
                    Surface(
                        onClick = { showAddDialog = true },
                        color = NernalColors.CardInner,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NernalColors.Gold.copy(alpha = 0.8f)),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Task or Event",
                                tint = NernalColors.Gold,
                                modifier = Modifier.size(20.dp),
                            )
                            Text("Add", color = NernalColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showAddDialog) {
                    AddReminderDialog(
                        initialCategory = if (state.hubFilter == HubFilter.EVENTS) ReminderCategory.EVENT else ReminderCategory.TASK,
                        onDismiss = { showAddDialog = false },
                        onAdd = { t, c -> onAddReminder(t, c) },
                    )
                }

                HorizontalDivider(color = NernalColors.BorderSubtle)

                // Main Stage Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (state.hubFilter) {
                        HubFilter.EVENTS -> {
                            when (state.calendarViewMode) {
                                CalendarViewMode.MONTH -> {
                                    MonthCalendarView(
                                        reminders = state.reminders,
                                        availableCalendars = state.availableCalendars,
                                        todayKey = state.todayKey,
                                        onEventClick = onEventClick,
                                        onToggleReminder = onToggleReminder,
                                        isPrimaryView = true,
                                    )
                                }
                                CalendarViewMode.WEEK -> {
                                    WeekCalendarView(
                                        reminders = state.reminders,
                                        availableCalendars = state.availableCalendars,
                                        todayKey = state.todayKey,
                                        onEventClick = onEventClick,
                                        onToggleReminder = onToggleReminder,
                                        isPrimaryView = true,
                                    )
                                }
                                CalendarViewMode.THREE_DAY -> {
                                    ThreeDayCalendarView(
                                        reminders = state.reminders,
                                        availableCalendars = state.availableCalendars,
                                        todayKey = state.todayKey,
                                        onEventClick = onEventClick,
                                        onToggleReminder = onToggleReminder,
                                        isPrimaryView = true,
                                    )
                                }
                                CalendarViewMode.ITINERARY -> {
                                    OmniHubCard(
                                        reminders = state.reminders,
                                        activeFilter = HubFilter.EVENTS,
                                        calendarViewMode = CalendarViewMode.ITINERARY,
                                        todayKey = state.todayKey,
                                        availableTaskLists = state.availableTaskLists,
                                        selectedTaskListIds = state.selectedTaskListIds,
                                        activeTaskListFilters = state.activeTaskListFilters,
                                        availableCalendars = state.availableCalendars,
                                        onToggleActiveTaskListFilter = onToggleActiveTaskListFilter,
                                        onFilterChange = onFilterChange,
                                        onCalendarViewModeChange = onCalendarViewModeChange,
                                        onAddReminder = onAddReminder,
                                        onToggleReminder = onToggleReminder,
                                        onDeleteReminder = onDeleteReminder,
                                        onEventClick = onEventClick,
                                        showHeader = false,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                        else -> {
                            // Tasks / All views
                            OmniHubCard(
                                reminders = state.reminders,
                                activeFilter = state.hubFilter,
                                calendarViewMode = state.calendarViewMode,
                                todayKey = state.todayKey,
                                availableTaskLists = state.availableTaskLists,
                                selectedTaskListIds = state.selectedTaskListIds,
                                activeTaskListFilters = state.activeTaskListFilters,
                                availableCalendars = state.availableCalendars,
                                onToggleActiveTaskListFilter = onToggleActiveTaskListFilter,
                                onFilterChange = onFilterChange,
                                onCalendarViewModeChange = onCalendarViewModeChange,
                                onAddReminder = onAddReminder,
                                onToggleReminder = onToggleReminder,
                                onDeleteReminder = onDeleteReminder,
                                onEventClick = onEventClick,
                                showHeader = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
