package com.nernal.frame.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class PendingAuthSession(
    val authUrl: String,
    val redirectUri: String,
    val codeVerifier: String,
    val state: String,
    val lanHelperUrl: String?,
    val serverPort: Int?,
)

data class ParsedOAuthResponse(
    val code: String? = null,
    val state: String? = null,
    val error: String? = null,
)

sealed interface TokenResult {
    data class Valid(val accessToken: String) : TokenResult
    data class TransientFailure(val message: String) : TokenResult
    data class AuthRevoked(val message: String) : TokenResult
}

class GoogleAuthManager(
    private val prefs: FramePrefs,
    private val client: OkHttpClient,
) {
    private val tokenMutex = Mutex()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var activeSession: PendingAuthSession? = null
    private var keyServerSocket: ServerSocket? = null
    private var keyServerJob: Job? = null

    fun startAuthSession(
        scope: CoroutineScope,
        onCallbackReceived: (String) -> Unit,
    ): Result<PendingAuthSession> = runCatching {
        stopAuthServer()

        val clientId = prefs.loadOAuthClientId().trim()
        require(clientId.isNotBlank()) {
            "OAuth Client ID is empty. Open Client Config below and enter your Google OAuth Client ID."
        }

        val bound = bindFirstAvailablePort(listOf(3000, 8080, 8089))
        val port = bound?.localPort
        // Default to http://localhost:3000/auth/callback so the Web OAuth client works seamlessly
        // even in the in-app WebView or manual code-paste fallback if port 3000 was busy.
        val redirectUri = defaultRedirectUriFor(clientId, port)

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = computeCodeChallenge(codeVerifier)
        val state = generateCodeVerifier().take(24)
        prefs.savePendingCodeVerifier(codeVerifier)
        prefs.savePendingOAuthState(state)
        prefs.savePendingRedirectUri(redirectUri)

        val authUrl = buildAuthorizationUrl(
            clientId = clientId,
            redirectUri = redirectUri,
            codeChallenge = codeChallenge,
            state = state,
        )

        val lanIp = getDeviceLanIp()
        val lanHelperUrl = if (port != null && lanIp != null) "http://$lanIp:$port/start" else null

        val session = PendingAuthSession(
            authUrl = authUrl,
            redirectUri = redirectUri,
            codeVerifier = codeVerifier,
            state = state,
            lanHelperUrl = lanHelperUrl,
            serverPort = port,
        )
        activeSession = session

        if (bound != null) {
            serverSocket = bound
            serverJob = scope.launch(Dispatchers.IO) {
                runCatching {
                    while (isActive && !bound.isClosed) {
                        val socket = try {
                            bound.accept()
                        } catch (_: SocketException) {
                            break
                        }
                        launch(Dispatchers.IO) {
                            handleHttpConnection(socket, session, onCallbackReceived)
                        }
                    }
                }
            }
        }

        session
    }

    fun stopAuthServer() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    /** Starts a lightweight local Wi-Fi form so a user can paste a Gemini API key from their phone or laptop. */
    fun startGeminiKeyServer(
        scope: CoroutineScope,
        onKeyReceived: (String) -> Unit,
    ): Result<String> = runCatching {
        stopGeminiKeyServer()
        val lanIp = getDeviceLanIp() ?: error("Connect this tablet to Wi-Fi first.")
        val bound = bindFirstAvailablePort(listOf(8091, 8092, 8093, 3001))
            ?: error("Could not open a local Wi-Fi port for key transfer.")
        val url = "http://$lanIp:${bound.localPort}/key"
        keyServerSocket = bound
        keyServerJob = scope.launch(Dispatchers.IO) {
            runCatching {
                while (isActive && !bound.isClosed) {
                    val socket = try {
                        bound.accept()
                    } catch (_: SocketException) {
                        break
                    }
                    launch(Dispatchers.IO) {
                        handleGeminiKeyHttpConnection(socket, onKeyReceived)
                    }
                }
            }
        }
        url
    }

    fun stopGeminiKeyServer() {
        keyServerJob?.cancel()
        keyServerJob = null
        runCatching { keyServerSocket?.close() }
        keyServerSocket = null
    }

    private fun handleGeminiKeyHttpConnection(
        socket: java.net.Socket,
        onKeyReceived: (String) -> Unit,
    ) {
        runCatching {
            socket.use { sock ->
                sock.soTimeout = 15_000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                val requestLine = reader.readLine() ?: return@use
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0)?.uppercase() ?: "GET"
                val path = parts.getOrNull(1) ?: "/"

                var contentLength = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull()?.coerceIn(0, 8192) ?: 0
                    }
                }

                val postBody = if (method == "POST" && contentLength > 0) {
                    val buf = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val n = reader.read(buf, read, contentLength - read)
                        if (n <= 0) break
                        read += n
                    }
                    String(buf, 0, read)
                } else {
                    ""
                }

                val appLabel = com.nernal.frame.BuildConfig.APP_DISPLAY_NAME
                var savedKey: String? = null
                if (method == "POST" && path.startsWith("/key")) {
                    for (pair in postBody.split("&")) {
                        val eq = pair.indexOf('=')
                        if (eq <= 0) continue
                        val k = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                        val v = URLDecoder.decode(pair.substring(eq + 1), "UTF-8").trim()
                        if (k == "apikey" && v.isNotBlank()) {
                            savedKey = NernalRepository.GEMINI_API_KEY_REGEX.find(v)?.value ?: v
                        }
                    }
                }

                val html = if (savedKey != null) {
                    """
                    <!DOCTYPE html>
                    <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>$appLabel Gemini Key Saved</title></head>
                    <body style="font-family:sans-serif;background:#101113;color:#faf8f5;padding:36px;max-width:520px;margin:0 auto;text-align:center;">
                    <h1 style="color:#34d399;">Gemini API Key Saved</h1>
                    <p style="color:#bbb7af;font-size:15px;">Super $appLabel is now enabled on your tablet. You can close this tab.</p>
                    </body></html>
                    """.trimIndent()
                } else {
                    """
                    <!DOCTYPE html>
                    <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>$appLabel Gemini Key</title></head>
                    <body style="font-family:sans-serif;background:#101113;color:#faf8f5;padding:24px;max-width:520px;margin:0 auto;">
                    <h2 style="color:#f5f2eb;margin-bottom:8px;">Send Gemini API Key to $appLabel</h2>
                    <p style="color:#bbb7af;font-size:14px;line-height:1.5;">
                    Paste your Gemini API key from <a href="https://aistudio.google.com/apikey" target="_blank" style="color:#60a5fa;">Google AI Studio</a> below. It transfers directly over your local Wi-Fi straight to this tablet.
                    </p>
                    <form method="POST" action="/key" style="background:#18191c;border:1px solid #2e3036;border-radius:12px;padding:16px;margin-top:16px;">
                    <label for="apikey" style="display:block;color:#f5f2eb;font-weight:bold;font-size:13px;margin-bottom:8px;">Gemini API Key</label>
                    <input id="apikey" name="apikey" type="password" placeholder="AIzaSy..." required autocomplete="off" style="width:100%;box-sizing:border-box;padding:12px;border-radius:8px;border:1px solid #2e3036;background:#101113;color:#faf8f5;font-size:15px;margin-bottom:14px;">
                    <button type="submit" style="width:100%;padding:14px;border:none;border-radius:10px;background:#f5f2eb;color:#101113;font-weight:bold;font-size:16px;cursor:pointer;">Save Key to Tablet</button>
                    </form>
                    </body></html>
                    """.trimIndent()
                }

                val bodyBytes = html.toByteArray(StandardCharsets.UTF_8)
                val headers = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ${bodyBytes.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }.toByteArray(StandardCharsets.US_ASCII)
                val out = sock.getOutputStream()
                out.write(headers)
                out.write(bodyBytes)
                out.flush()

                if (savedKey != null) {
                    onKeyReceived(savedKey)
                    stopGeminiKeyServer()
                }
            }
        }
    }

    private fun handleHttpConnection(
        socket: java.net.Socket,
        session: PendingAuthSession,
        onCallbackReceived: (String) -> Unit,
    ) {
        runCatching {
            socket.use { sock ->
                sock.soTimeout = 15_000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                val requestLine = reader.readLine() ?: return@use
                val parts = requestLine.split(" ")
                val path = parts.getOrNull(1) ?: "/"

                // Drain headers
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }

                val out = sock.getOutputStream()
                val port = session.serverPort ?: 3000
                val appLabel = com.nernal.frame.BuildConfig.APP_DISPLAY_NAME
                val authScheme = com.nernal.frame.BuildConfig.AUTH_SCHEME
                val appPackage = com.nernal.frame.BuildConfig.APPLICATION_ID
                val returnIntentUrl =
                    "intent://auth-complete#Intent;scheme=$authScheme;package=$appPackage;S.browser_fallback_url=http%3A%2F%2Flocalhost%3A$port%2F;end"

                if (path.startsWith("/start")) {
                    val escapedAuthUrl = session.authUrl
                        .replace("&", "&amp;")
                        .replace("\"", "&quot;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    val body = """
                        <!DOCTYPE html>
                        <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                        <title>$appLabel Google Sign-In</title></head>
                        <body style="font-family:sans-serif;background:#101113;color:#faf8f5;padding:24px;max-width:520px;margin:0 auto;">
                        <h2 style="color:#f5f2eb;margin-bottom:8px;">$appLabel Sign-In</h2>
                        <p style="color:#bbb7af;font-size:14px;line-height:1.5;">
                        1. Tap below to sign in with Google and approve permissions.<br>
                        2. After approving, Google tries to redirect to <code>localhost</code> on this phone. Copy that full <code>localhost</code> URL from your address bar and paste it into Step 2 below.
                        </p>
                        <p style="margin:20px 0;">
                        <a href="$escapedAuthUrl" style="display:inline-block;background:#f5f2eb;color:#101113;font-weight:bold;text-decoration:none;padding:14px 22px;border-radius:10px;">1. Open Google Sign-In</a>
                        </p>
                        <form method="GET" action="/auth/callback" style="background:#18191c;border:1px solid #2e3036;border-radius:12px;padding:16px;">
                        <label for="paste" style="display:block;color:#60a5fa;font-weight:bold;font-size:13px;margin-bottom:8px;">2. Paste Redirect URL or Code</label>
                        <input id="paste" name="paste" type="text" placeholder="http://localhost:3000/auth/callback?..." required style="width:100%;box-sizing:border-box;padding:12px;border-radius:8px;border:1px solid #2e3036;background:#101113;color:#faf8f5;font-size:14px;margin-bottom:12px;">
                        <button type="submit" style="width:100%;padding:13px;border:none;border-radius:8px;background:#60a5fa;color:#082f49;font-weight:bold;font-size:15px;cursor:pointer;">Connect $appLabel</button>
                        </form>
                        </body></html>
                    """.trimIndent().toByteArray(StandardCharsets.UTF_8)
                    val headers = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: text/html; charset=utf-8\r\n")
                        append("Content-Length: ${body.size}\r\n")
                        append("Connection: close\r\n\r\n")
                    }.toByteArray(StandardCharsets.US_ASCII)
                    out.write(headers)
                    out.write(body)
                    out.flush()
                    return@use
                }

                val parsed = extractAuthCodeAndState(path)
                val isError = !parsed.error.isNullOrBlank()
                val hasCode = !parsed.code.isNullOrBlank()
                val isLanPaste = path.contains("paste=")
                val autoReturnScript = if ((hasCode || isError) && !isLanPaste) {
                    """<script>setTimeout(function(){window.location.href="$returnIntentUrl";},250);</script>"""
                } else {
                    ""
                }
                val returnButtonHtml = if (!isLanPaste) {
                    """<p style="margin-top:24px;"><a href="$returnIntentUrl" style="display:inline-block;background:#f5f2eb;color:#101113;font-weight:bold;text-decoration:none;padding:16px 28px;border-radius:12px;font-size:18px;">Return to $appLabel</a></p>"""
                } else {
                    ""
                }

                val html = if (hasCode) {
                    """
                    <!DOCTYPE html>
                    <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$appLabel Connected</title></head>
                    <body style="font-family:sans-serif;background:#101113;color:#faf8f5;padding:40px;text-align:center;">
                    <h1 style="color:#34d399;">Google Account Connected</h1>
                    <p style="color:#bbb7af;">${if (isLanPaste) "You can close this browser tab and return to $appLabel." else "Returning to $appLabel. If it does not switch automatically, tap the button below."}</p>
                    $returnButtonHtml
                    $autoReturnScript
                    </body></html>
                    """.trimIndent()
                } else {
                    """
                    <!DOCTYPE html>
                    <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$appLabel Sign-In</title></head>
                    <body style="font-family:sans-serif;background:#101113;color:#faf8f5;padding:40px;text-align:center;">
                    <h2 style="color:#fbbf24;">${if (isError) "Sign-In Cancelled (${parsed.error})" else "Waiting for Google Sign-In"}</h2>
                    <p style="color:#bbb7af;">Return to $appLabel to try again.</p>
                    $returnButtonHtml
                    $autoReturnScript
                    </body></html>
                    """.trimIndent()
                }
                val bodyBytes = html.toByteArray(StandardCharsets.UTF_8)
                val headers = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ${bodyBytes.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }.toByteArray(StandardCharsets.US_ASCII)
                out.write(headers)
                out.write(bodyBytes)
                out.flush()

                if (hasCode || isError) {
                    onCallbackReceived("http://localhost:${session.serverPort ?: 3000}$path")
                }
            }
        }
    }

    suspend fun exchangeCodeForTokens(rawCodeOrUrl: String): Result<Pair<GoogleTokens, GoogleAccountProfile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val session = activeSession
                val clientId = prefs.loadOAuthClientId().trim()
                val clientSecret = prefs.loadOAuthClientSecret().trim()
                require(clientId.isNotBlank()) { "Missing OAuth Client ID" }

                val parsed = extractAuthCodeAndState(rawCodeOrUrl)
                if (!parsed.error.isNullOrBlank()) {
                    error("Google sign-in declined: ${parsed.error}")
                }
                val code = parsed.code?.takeIf { it.isNotBlank() }
                    ?: error("Could not find an authorization code in that input.")

                val expectedState = session?.state ?: prefs.loadPendingOAuthState()
                val hasCallbackParams = rawCodeOrUrl.contains("?") ||
                    rawCodeOrUrl.contains("code=") ||
                    rawCodeOrUrl.contains("paste=")
                if (!expectedState.isNullOrBlank()) {
                    if ((!parsed.state.isNullOrBlank() || hasCallbackParams) && parsed.state != expectedState) {
                        error("OAuth state mismatch. Please start sign-in again.")
                    }
                }

                val redirectUri = session?.redirectUri
                    ?: prefs.loadPendingRedirectUri()
                    ?: defaultRedirectUriFor(clientId, null)
                val formBuilder = FormBody.Builder()
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("redirect_uri", redirectUri)
                    .add("grant_type", "authorization_code")

                if (clientSecret.isNotBlank()) {
                    formBuilder.add("client_secret", clientSecret)
                }
                val verifier = session?.codeVerifier
                    ?: prefs.loadPendingCodeVerifier()
                if (!verifier.isNullOrBlank()) {
                    formBuilder.add("code_verifier", verifier)
                }

                val req = Request.Builder()
                    .url(TOKEN_URL)
                    .post(formBuilder.build())
                    .build()

                val respJson = client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val errMsg = runCatching { JSONObject(text).optString("error_description") }
                            .getOrNull()?.takeIf { it.isNotBlank() }
                            ?: runCatching { JSONObject(text).optString("error") }.getOrNull()
                            ?: "Token exchange failed (HTTP ${resp.code})"
                        error(errMsg)
                    }
                    JSONObject(text)
                }

                val accessToken = respJson.getString("access_token")
                val refreshToken = respJson.optString("refresh_token").takeIf { it.isNotBlank() }
                    ?: prefs.loadGoogleTokens()?.refreshToken
                val expiresInSec = respJson.optLong("expires_in", 3600L)
                val tokens = GoogleTokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L,
                )

                val profile = fetchUserProfile(accessToken)
                prefs.saveGoogleTokens(tokens)
                prefs.saveAccountProfile(profile)
                prefs.clearPendingOAuthSession()
                stopAuthServer()
                tokens to profile
            }
        }

    suspend fun getAccessTokenResult(): TokenResult = tokenMutex.withLock {
        val current = prefs.loadGoogleTokens()
            ?: return@withLock TokenResult.AuthRevoked("Google session not found — please sign in.")
        if (!current.isExpiredOrExpiringSoon()) {
            return@withLock TokenResult.Valid(current.accessToken)
        }
        val refresh = current.refreshToken?.takeIf { it.isNotBlank() }
            ?: return@withLock TokenResult.AuthRevoked("Google session expired — please sign in again.")
        val clientId = prefs.loadOAuthClientId().trim()
        val clientSecret = prefs.loadOAuthClientSecret().trim()
        if (clientId.isBlank()) {
            return@withLock TokenResult.AuthRevoked("OAuth Client ID is empty.")
        }

        return@withLock withContext(Dispatchers.IO) {
            try {
                val formBuilder = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("refresh_token", refresh)
                    .add("grant_type", "refresh_token")
                if (clientSecret.isNotBlank()) {
                    formBuilder.add("client_secret", clientSecret)
                }

                val req = Request.Builder()
                    .url(TOKEN_URL)
                    .post(formBuilder.build())
                    .build()

                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val errJson = runCatching { JSONObject(text) }.getOrNull()
                        val errCode = errJson?.optString("error").orEmpty()
                        val errDesc = errJson?.optString("error_description").orEmpty()
                        val summary = errDesc.ifBlank { errCode }.ifBlank { "HTTP ${resp.code}" }
                        return@withContext if (resp.code == 400 || resp.code == 401 || errCode == "invalid_grant") {
                            TokenResult.AuthRevoked("Google session expired ($summary) — please sign in again.")
                        } else {
                            TokenResult.TransientFailure("Google auth temporarily unavailable ($summary)")
                        }
                    }

                    val respJson = JSONObject(text)
                    val newAccess = respJson.optString("access_token").takeIf { it.isNotBlank() }
                        ?: return@withContext TokenResult.TransientFailure("Empty access token in refresh response")
                    val expiresInSec = respJson.optLong("expires_in", 3600L)
                    val updated = GoogleTokens(
                        accessToken = newAccess,
                        refreshToken = respJson.optString("refresh_token").takeIf { it.isNotBlank() } ?: refresh,
                        expiresAtMs = System.currentTimeMillis() + expiresInSec * 1000L,
                    )
                    prefs.saveGoogleTokens(updated)
                    TokenResult.Valid(newAccess)
                }
            } catch (e: IOException) {
                TokenResult.TransientFailure(e.message ?: "Network unavailable")
            } catch (e: Exception) {
                TokenResult.TransientFailure(e.message ?: "Token refresh failed")
            }
        }
    }

    suspend fun getValidAccessToken(): String? =
        (getAccessTokenResult() as? TokenResult.Valid)?.accessToken

    suspend fun fetchUserProfile(accessToken: String): GoogleAccountProfile = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(USERINFO_URL)
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("User profile lookup failed (HTTP ${resp.code})")
            val root = JSONObject(resp.body?.string() ?: error("Empty userinfo response"))
            val email = root.optString("email").ifBlank { "Google Account" }
            val name = root.optString("name").ifBlank { email }
            val avatar = root.optString("picture").takeIf { it.isNotBlank() }
            GoogleAccountProfile(email = email, displayName = name, avatarUrl = avatar)
        }
    }

    private fun bindFirstAvailablePort(ports: List<Int>): ServerSocket? {
        for (port in ports) {
            try {
                return ServerSocket(port, 8).apply {
                    reuseAddress = true
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun getDeviceLanIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.filter { it.isUp && !it.isLoopback }
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { addr -> addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()

    companion object {
        private const val AUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
        const val DEFAULT_REDIRECT_URI = "http://localhost:3000/auth/callback"

        fun defaultRedirectUriFor(@Suppress("UNUSED_PARAMETER") clientId: String = "", boundPort: Int?): String {
            if (boundPort == null || boundPort == 3000) {
                return DEFAULT_REDIRECT_URI
            }
            return "http://localhost:$boundPort"
        }

        val OAUTH_SCOPES = listOf(
            "openid",
            "email",
            "profile",
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/tasks",
            "https://www.googleapis.com/auth/photospicker.mediaitems.readonly",
        )

        fun generateCodeVerifier(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun computeCodeChallenge(verifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }

        fun buildAuthorizationUrl(
            clientId: String,
            redirectUri: String,
            codeChallenge: String,
            state: String,
        ): String {
            val params = linkedMapOf(
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "response_type" to "code",
                "scope" to OAUTH_SCOPES.joinToString(" "),
                "access_type" to "offline",
                "prompt" to "consent",
                "include_granted_scopes" to "true",
                "code_challenge" to codeChallenge,
                "code_challenge_method" to "S256",
                "state" to state,
            )
            val query = params.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            return "$AUTH_BASE_URL?$query"
        }

        fun extractAuthCodeAndState(rawInput: String): ParsedOAuthResponse {
            return extractAuthCodeAndStateInternal(rawInput, depth = 0)
        }

        private fun extractAuthCodeAndStateInternal(rawInput: String, depth: Int): ParsedOAuthResponse {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty() || depth > 2) return ParsedOAuthResponse()

            val query = when {
                trimmed.contains("?") -> trimmed.substringAfter("?").substringBefore("#")
                trimmed.contains("code=") || trimmed.contains("error=") || trimmed.contains("paste=") ->
                    trimmed.substringBefore("#")
                else -> return ParsedOAuthResponse(code = trimmed)
            }

            var code: String? = null
            var state: String? = null
            var error: String? = null
            var paste: String? = null
            for (pair in query.split("&")) {
                val eq = pair.indexOf('=')
                if (eq <= 0) continue
                val key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                when (key) {
                    "code" -> code = value
                    "state" -> state = value
                    "error" -> error = value
                    "paste" -> paste = value
                }
            }
            if (!paste.isNullOrBlank()) {
                return extractAuthCodeAndStateInternal(paste, depth + 1)
            }
            return ParsedOAuthResponse(code = code, state = state, error = error)
        }
    }
}
