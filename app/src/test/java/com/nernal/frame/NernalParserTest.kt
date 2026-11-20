package com.nernal.frame

import com.nernal.frame.data.AlbumPhoto
import com.nernal.frame.data.FramePrefs
import com.nernal.frame.data.GoogleAuthManager
import com.nernal.frame.data.NernalRepository
import com.nernal.frame.data.ReminderCategory
import com.nernal.frame.data.ReminderItem
import com.nernal.frame.data.WeatherConditionKind
import com.nernal.frame.data.isEventWithinReminderWindow
import com.nernal.frame.data.selectNextUpcomingEvent
import com.nernal.frame.data.shouldAutoCloseEventAlert
import com.nernal.frame.data.sortReminders
import com.nernal.frame.data.wmoDescription
import com.nernal.frame.data.wmoKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class NernalParserTest {

    @Test
    fun albumPhotoDisplayUrlAddsSizingForGooglePhotos() {
        val photo = AlbumPhoto(
            id = "p1",
            url = "https://lh3.googleusercontent.com/pw/AP1GczTestTokenHere",
            width = 4032,
            height = 3024,
            timestampMs = null,
            year = 2025,
            albumTitle = "Summer Trip",
        )
        assertEquals(
            "https://lh3.googleusercontent.com/pw/AP1GczTestTokenHere=w1280-h800",
            photo.displayUrl(),
        )
    }

    @Test
    fun parsesGoogleCalendarListAndEvents() {
        val calJson = """
            {
              "items": [
                { "id": "family@group.calendar.google.com", "summary": "Family Calendar", "backgroundColor": "#9fe1e7", "primary": false, "accessRole": "writer" },
                { "id": "user@gmail.com", "summary": "Personal", "primary": true, "accessRole": "owner" }
              ]
            }
        """.trimIndent()
        val cals = NernalRepository.parseCalendarListJson(calJson)
        assertEquals(2, cals.size)
        assertEquals("user@gmail.com", cals.first().id)
        assertTrue(cals.first().primary)
        assertTrue(cals.last().isWritable)

        val eventsJson = """
            {
              "items": [
                {
                  "id": "ev1",
                  "status": "confirmed",
                  "summary": "Dentist Appointment",
                  "start": { "dateTime": "2026-11-15T14:30:00-05:00" }
                },
                {
                  "id": "ev2",
                  "status": "confirmed",
                  "summary": "School Holiday",
                  "start": { "date": "2026-11-20" }
                },
                {
                  "id": "ev3",
                  "status": "cancelled",
                  "summary": "Canceled Meeting",
                  "start": { "date": "2026-11-21" }
                }
              ]
            }
        """.trimIndent()
        val parsed = NernalRepository.parseCalendarEventsJson(
            jsonText = eventsJson,
            calendarId = "user@gmail.com",
            zoneId = ZoneId.of("America/New_York"),
        )
        assertEquals(2, parsed.size)
        assertEquals("Dentist Appointment", parsed[0].text)
        assertEquals("2026-11-15", parsed[0].dateKey)
        assertEquals("2:30 PM", parsed[0].time)
        assertEquals(ReminderCategory.EVENT, parsed[0].category)
        assertEquals("user@gmail.com", parsed[0].sourceId)
        assertEquals("ev1", parsed[0].remoteId)

        assertEquals("School Holiday", parsed[1].text)
        assertEquals("2026-11-20", parsed[1].dateKey)
        assertNull(parsed[1].time)
    }

    @Test
    fun parsesGoogleTaskListsAndTasks() {
        val listsJson = """
            {
              "items": [
                { "id": "list_default", "title": "My Tasks" },
                { "id": "list_groceries", "title": "Errands" }
              ]
            }
        """.trimIndent()
        val lists = NernalRepository.parseTaskListsJson(listsJson)
        assertEquals(2, lists.size)
        assertEquals("Errands", lists[1].title)

        val tasksJson = """
            {
              "items": [
                { "id": "t1", "title": "Pay electric bill", "status": "needsAction", "due": "2026-11-16T00:00:00.000Z" },
                { "id": "t2", "title": "Milk & Eggs", "status": "completed", "updated": "2026-11-14T18:00:00.000Z" },
                { "id": "t3", "title": "Deleted item", "status": "needsAction", "deleted": true }
              ]
            }
        """.trimIndent()
        val parsedTasks = NernalRepository.parseTasksListJson(tasksJson, "list_default", ReminderCategory.TASK)
        assertEquals(2, parsedTasks.size)
        assertFalse(parsedTasks[0].completed)
        assertEquals("2026-11-16", parsedTasks[0].dateKey)
        assertEquals(ReminderCategory.TASK, parsedTasks[0].category)
        assertTrue(parsedTasks[1].completed)
        assertEquals("t2", parsedTasks[1].remoteId)
    }

    @Test
    fun parsesGooglePhotosAlbumsAndMediaItems() {
        val albumsJson = """
            {
              "albums": [
                { "id": "alb1", "title": "Family Trip", "mediaItemsCount": "42", "coverPhotoBaseUrl": "https://lh3.googleusercontent.com/cover1" }
              ],
              "nextPageToken": "tok_next"
            }
        """.trimIndent()
        val (albums, nextAlbumTok) = NernalRepository.parsePhotoAlbumsJson(albumsJson, "albums", isShared = false)
        assertEquals(1, albums.size)
        assertEquals("Family Trip", albums.first().title)
        assertEquals(42, albums.first().mediaItemsCount)
        assertEquals("tok_next", nextAlbumTok)

        val mediaJson = """
            {
              "mediaItems": [
                {
                  "id": "m1",
                  "baseUrl": "https://lh3.googleusercontent.com/lr/item1",
                  "mimeType": "image/jpeg",
                  "mediaMetadata": { "width": "4000", "height": "3000", "creationTime": "2025-07-04T15:00:00Z" }
                },
                {
                  "id": "m2",
                  "baseUrl": "https://lh3.googleusercontent.com/lr/vid1",
                  "mimeType": "video/mp4",
                  "mediaMetadata": { "width": "1920", "height": "1080" }
                }
              ]
            }
        """.trimIndent()
        val (photos, nextMediaTok) = NernalRepository.parseMediaItemsJson(mediaJson, "Family Trip", "alb1")
        assertNull(nextMediaTok)
        assertEquals(1, photos.size)
        assertEquals("m1", photos.first().id)
        assertEquals(2025, photos.first().year)
        assertEquals("https://lh3.googleusercontent.com/lr/item1=w1280-h800", photos.first().displayUrl())
    }

    @Test
    fun pkceChallengeAndOAuthCodeExtractionWork() {
        val verifier = GoogleAuthManager.generateCodeVerifier()
        val challenge = GoogleAuthManager.computeCodeChallenge(verifier)
        assertTrue(verifier.length >= 43)
        assertNotEquals(verifier, challenge)
        assertFalse(challenge.contains("="))

        val defaultRedirect = GoogleAuthManager.defaultRedirectUriFor(
            "test-client-id.apps.googleusercontent.com",
            3000,
        )
        assertEquals("http://localhost:3000/auth/callback", defaultRedirect)

        val url = GoogleAuthManager.buildAuthorizationUrl(
            clientId = "test-client-id",
            redirectUri = defaultRedirect,
            codeChallenge = challenge,
            state = "xyz123",
        )
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("calendar"))
        assertTrue(url.contains("tasks"))

        val parsedUrl = GoogleAuthManager.extractAuthCodeAndState(
            "http://localhost:3000/auth/callback?state=xyz123&code=4%2F0TestAuthCode&scope=email"
        )
        assertEquals("4/0TestAuthCode", parsedUrl.code)
        assertEquals("xyz123", parsedUrl.state)
        assertNull(parsedUrl.error)

        val parsedRaw = GoogleAuthManager.extractAuthCodeAndState("4/0RawCodeOnly")
        assertEquals("4/0RawCodeOnly", parsedRaw.code)

        val parsedErr = GoogleAuthManager.extractAuthCodeAndState("http://localhost:8080/?error=access_denied")
        assertEquals("access_denied", parsedErr.error)
    }

    @Test
    fun selectBalancedPhotosPreservesAllFavoritesAndBalancesAlbums() {
        val a1 = (1..20).map {
            AlbumPhoto("a-$it", "https://example.com/a/$it", 100, 100, null, 2024, "Album One", "alb1")
        }
        val a2 = (1..20).map {
            AlbumPhoto("b-$it", "https://example.com/b/$it", 100, 100, null, 2025, "Album Two", "alb2")
        }
        val favs = setOf("a-1", "b-2")
        val selected = NernalRepository.selectBalancedPhotos(a1 + a2, maxCount = 10, favorites = favs)
        assertEquals(10, selected.size)
        val ids = selected.map { it.id }.toSet()
        assertTrue("a-1" in ids && "b-2" in ids)
        assertTrue(selected.any { it.albumTitle == "Album One" })
        assertTrue(selected.any { it.albumTitle == "Album Two" })
    }

    @Test
    fun iemNexradFramesAndUrlsBuildCleanly() {
        val data = NernalRepository.buildIemNexradData(nowSec = 1794698450L)
        assertEquals(11, data.frames.size)
        assertEquals("/nexrad-n0q-900913-m50m", data.frames.first().path)
        assertEquals("/nexrad-n0q-900913", data.frames.last().path)
        assertEquals(
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/6/18/24.png",
            NernalRepository.radarTileUrl(data.host, data.frames.last().path, 6, 18, 24),
        )
        assertEquals(
            "https://tilecache.rainviewer.com/v2/radar/abc/256/6/18/24/2/1_1.png",
            NernalRepository.radarTileUrl("https://tilecache.rainviewer.com", "/v2/radar/abc", 6, 18, 24),
        )
    }

    @Test
    fun localPhotoFiltersAndWmoCodesWork() {
        val photos = listOf(
            AlbumPhoto("1", "https://example.com/1", 100, 100, null, 2024, "Trip"),
            AlbumPhoto("2", "https://example.com/2", 100, 100, null, 2025, "Family"),
            AlbumPhoto("3", "https://example.com/3", 100, 100, null, 2025, "Trip"),
        )
        assertEquals(10, FramePrefs.DEFAULT_SLIDE_INTERVAL_SEC)
        assertEquals(listOf("2", "3"), NernalRepository.filterPhotos(photos, yearFilter = 2025).map { it.id })
        assertEquals(listOf("2"), NernalRepository.filterPhotos(photos, yearFilter = 2025, albumFilter = "Family").map { it.id })
        assertEquals("Clear Sky", wmoDescription(0))
        assertEquals(WeatherConditionKind.RAIN, wmoKind(63))
        assertTrue(NernalRepository.calculateInSampleSize(4000, 3000, 1280, 800) >= 2)
    }

    @Test
    fun autoDetectPhotoLinksFromTaskTextAndNotes() {
        val regex = NernalRepository.PHOTO_LINK_REGEX
        val sampleTask = "Photos: https://photos.app.goo.gl/AbCdEfGh12345"
        val sampleNotes = "Check this album out: https://photos.google.com/share/AF1QipN9_87654?key=abc; and enjoy"

        val matches = mutableListOf<String>()
        regex.findAll(sampleTask).forEach { matches.add(it.value.trimEnd('.', ',', ')', '>', ';')) }
        regex.findAll(sampleNotes).forEach { matches.add(it.value.trimEnd('.', ',', ')', '>', ';')) }

        assertEquals(2, matches.size)
        assertEquals("https://photos.app.goo.gl/AbCdEfGh12345", matches[0])
        assertEquals("https://photos.google.com/share/AF1QipN9_87654?key=abc", matches[1])
    }

    @Test
    fun sortRemindersUsesChronologicalAmPmOrderAndSelectsUpcomingEvent() {
        val afternoon = ReminderItem(
            id = "e1",
            text = "Afternoon Dentist",
            category = ReminderCategory.EVENT,
            dateKey = "2026-06-15",
            time = "1:15 PM",
            createdAtMs = 1L,
        )
        val morning = ReminderItem(
            id = "e2",
            text = "Morning Standup",
            category = ReminderCategory.EVENT,
            dateKey = "2026-06-15",
            time = "9:30 AM",
            createdAtMs = 2L,
        )
        val untimed = ReminderItem(
            id = "e3",
            text = "All Day Picnic",
            category = ReminderCategory.EVENT,
            dateKey = "2026-06-15",
            time = null,
            createdAtMs = 3L,
        )
        val tomorrow = ReminderItem(
            id = "e4",
            text = "Tomorrow Flight",
            category = ReminderCategory.EVENT,
            dateKey = "2026-06-16",
            time = "7:00 AM",
            createdAtMs = 4L,
        )

        val sorted = sortReminders(listOf(tomorrow, afternoon, untimed, morning))
        // All-day / untimed events sort to the top of the day
        assertEquals(listOf("e3", "e2", "e1", "e4"), sorted.map { it.id })

        // At 10:00 AM (600 min), morning 9:30 AM has already started so afternoon 1:15 PM should be selected.
        val nextAt10am = selectNextUpcomingEvent(sorted, todayKey = "2026-06-15", nowMinutes = 600)
        assertEquals("e1", nextAt10am?.id)

        // At 2:00 PM (840 min), both timed today events have started so today's all-day event is selected when present.
        val nextAt2pmWithAllDay = selectNextUpcomingEvent(sorted, todayKey = "2026-06-15", nowMinutes = 840)
        assertEquals("e3", nextAt2pmWithAllDay?.id)

        // When only timed events are present and today's have started, tomorrow's timed event is next.
        val nextAt2pmTimedOnly = selectNextUpcomingEvent(
            listOf(morning, afternoon, tomorrow),
            todayKey = "2026-06-15",
            nowMinutes = 840,
        )
        assertEquals("e4", nextAt2pmTimedOnly?.id)
    }

    @Test
    fun eventReminderWindowAndAutoCloseAtStartTime() {
        val today = "2026-06-15"
        val event = ReminderItem(
            id = "cal:primary:evt1",
            text = "School Pickup",
            category = ReminderCategory.EVENT,
            dateKey = today,
            time = "3:30 PM", // 15 * 60 + 30 = 930 minutes
        )

        // Window is 1..5 minutes before start (925..929 inclusive).
        org.junit.Assert.assertFalse(isEventWithinReminderWindow(event, today, nowMinutes = 924))
        org.junit.Assert.assertTrue(isEventWithinReminderWindow(event, today, nowMinutes = 925))
        org.junit.Assert.assertTrue(isEventWithinReminderWindow(event, today, nowMinutes = 929))
        org.junit.Assert.assertFalse(isEventWithinReminderWindow(event, today, nowMinutes = 930))

        val list = listOf(event)
        // Popup stays open 2 minutes before start when wake is enabled.
        org.junit.Assert.assertFalse(shouldAutoCloseEventAlert(event, list, today, nowMinutes = 928, wakeEnabled = true))
        // Popup auto-closes right at start time (930) and after start time (935).
        org.junit.Assert.assertTrue(shouldAutoCloseEventAlert(event, list, today, nowMinutes = 930, wakeEnabled = true))
        org.junit.Assert.assertTrue(shouldAutoCloseEventAlert(event, list, today, nowMinutes = 935, wakeEnabled = true))
        // Also closes if disabled, removed, or completed.
        org.junit.Assert.assertTrue(shouldAutoCloseEventAlert(event, list, today, nowMinutes = 928, wakeEnabled = false))
        org.junit.Assert.assertTrue(shouldAutoCloseEventAlert(event, emptyList(), today, nowMinutes = 928, wakeEnabled = true))
        org.junit.Assert.assertTrue(
            shouldAutoCloseEventAlert(
                event,
                listOf(event.copy(completed = true)),
                today,
                nowMinutes = 928,
                wakeEnabled = true,
            )
        )
    }

    @Test
    fun parseCalendarEventsExpandsMultiDayAllDayEvents() {
        val json = """
            {
              "items": [
                {
                  "id": "trip1",
                  "status": "confirmed",
                  "summary": "Beach Trip",
                  "start": { "date": "2026-07-10" },
                  "end": { "date": "2026-07-13" }
                }
              ]
            }
        """.trimIndent()

        val items = NernalRepository.parseCalendarEventsJson(
            jsonText = json,
            calendarId = "primary",
            zoneId = ZoneId.of("America/New_York"),
        )
        assertEquals(1, items.size)
        assertEquals("2026-07-10", items[0].dateKey)
        assertEquals("cal:primary:trip1", items[0].id)
        assertTrue(items[0].notes?.contains("Through 2026-07-12") == true)
    }

    @Test
    fun parseWeatherAndRainViewerJson() {
        val weatherJson = """
            {
              "current": {
                "temperature_2m": 74.6,
                "apparent_temperature": 76.2,
                "relative_humidity_2m": 58,
                "wind_speed_10m": 8.4,
                "weather_code": 2,
                "is_day": 1,
                "precipitation": 0.0
              },
              "daily": {
                "time": ["2026-06-15", "2026-06-16"],
                "weather_code": [2, 61],
                "temperature_2m_max": [82.1, 75.4],
                "temperature_2m_min": [61.9, 59.2],
                "precipitation_probability_max": [10, 70],
                "sunrise": ["2026-06-15T05:50"],
                "sunset": ["2026-06-15T20:35"]
              }
            }
        """.trimIndent()

        val weather = NernalRepository.parseWeatherJson(weatherJson, alerts = emptyList())
        assertEquals(75, weather.currentTempF)
        assertEquals(76, weather.apparentTempF)
        assertEquals(2, weather.daily.size)
        assertEquals("Today", weather.daily[0].dayLabel)
        assertEquals("Tue", weather.daily[1].dayLabel)
        assertEquals(70, weather.daily[1].precipChance)

        val radarJson = """
            {
              "host": "https://tilecache.rainviewer.com",
              "radar": {
                "past": [{ "time": 1700000000, "path": "/v2/radar/1700000000" }],
                "nowcast": [{ "time": 1700000600, "path": "/v2/radar/nowcast_1" }]
              }
            }
        """.trimIndent()
        val radar = NernalRepository.parseRainViewerJson(radarJson)
        assertEquals("https://tilecache.rainviewer.com", radar.host)
        assertEquals(2, radar.frames.size)
        org.junit.Assert.assertFalse(radar.frames[0].isForecast)
        org.junit.Assert.assertTrue(radar.frames[1].isForecast)
    }

    @Test
    fun parseSharedAlbumHtmlHelpersExtractTitleAndPhotos() {
        val html = """
            <html><head>
            <meta property="og:title" content="Summer Vacation &amp; Lake · Shared album">
            </head><body>
            <script>AF_initDataCallback({key: 'ds:1', hash: '1', data:[null,[["id1",["https://lh3.googleusercontent.com/abc",1600,1200],1700000000]], "tok2"], sideChannel: {}});</script>
            </body></html>
        """.trimIndent()

        assertEquals("Summer Vacation & Lake", NernalRepository.extractAlbumTitle(html))
        val (arr, nextTok) = NernalRepository.extractInitialItemsArray(html)
        org.junit.Assert.assertNotNull(arr)
        assertEquals("tok2", nextTok)

        val rawList = buildList {
            for (i in 0 until arr!!.length()) {
                add(arr.getJSONArray(i))
            }
        }
        val photos = NernalRepository.parseItemsToPhotos(rawList, albumTitle = "Summer Vacation & Lake", albumUrl = "https://photos.app.goo.gl/xyz")
        assertEquals(1, photos.size)
        assertEquals("id1", photos[0].id)
        assertEquals(1600, photos[0].width)
        assertEquals(1200, photos[0].height)
        assertEquals(1700000000000L, photos[0].timestampMs)
    }

    @Test
    fun appUpdateManagerVersionComparison() {
        assertTrue(com.nernal.frame.data.AppUpdateManager.isNewerVersion("v0.2.1", "0.2.0-oauth"))
        assertTrue(com.nernal.frame.data.AppUpdateManager.isNewerVersion("0.3.0", "0.2.0-oauth"))
        assertTrue(com.nernal.frame.data.AppUpdateManager.isNewerVersion("v1.0.0", "0.2.0-oauth"))
        assertFalse(com.nernal.frame.data.AppUpdateManager.isNewerVersion("v0.2.0", "0.2.0-oauth"))
        assertFalse(com.nernal.frame.data.AppUpdateManager.isNewerVersion("0.1.9", "0.2.0-oauth"))
        assertFalse(com.nernal.frame.data.AppUpdateManager.isNewerVersion("v0.2.0-oauth", "0.2.0-oauth"))
        assertFalse(com.nernal.frame.data.AppUpdateManager.isNewerVersion("", "0.2.0-oauth"))
    }
}
