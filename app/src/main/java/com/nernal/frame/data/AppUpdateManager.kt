package com.nernal.frame.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val tagName: String,
    val title: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val publishedAt: String,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class AppUpdateManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val repoOwner: String = "derallo",
    private val repoName: String = "onnkiosk",
) {
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            Log.d("AppUpdateManager", "Checking GitHub release at $url, currentVersion=$currentVersionName")
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Onnkiosk-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("AppUpdateManager", "GitHub release check HTTP code: ${response.code}")
                    if (response.code == 404) {
                        return@use UpdateCheckResult.UpToDate
                    }
                    return@use UpdateCheckResult.Error("GitHub releases API returned HTTP ${response.code}")
                }

                val bodyStr = response.body?.string() ?: return@use UpdateCheckResult.Error("Empty response from GitHub")
                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                val title = json.optString("name", tagName)
                val body = json.optString("body", "")
                val publishedAt = json.optString("published_at", "")

                val assets = json.optJSONArray("assets") ?: return@use UpdateCheckResult.UpToDate
                var apkUrl: String? = null
                var apkName: String? = null
                var apkSize = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkUrl.isNullOrBlank() || apkName.isNullOrBlank()) {
                    Log.d("AppUpdateManager", "Release $tagName found but no APK asset attached")
                    return@use UpdateCheckResult.UpToDate
                }

                val isNewer = isNewerVersion(remoteTag = tagName, currentVersion = currentVersionName)
                Log.d("AppUpdateManager", "Release tag=$tagName, isNewer=$isNewer, currentVersion=$currentVersionName")
                if (isNewer) {
                    UpdateCheckResult.UpdateAvailable(
                        AppReleaseInfo(
                            tagName = tagName,
                            title = title,
                            changelog = body,
                            apkDownloadUrl = apkUrl,
                            apkFileName = apkName,
                            apkSizeBytes = apkSize,
                            publishedAt = publishedAt,
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to check for updates", e)
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: AppReleaseInfo,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = context.externalCacheDir ?: context.cacheDir
            val destFile = File(downloadDir, "onnkiosk-update.apk")
            if (destFile.exists()) {
                destFile.delete()
            }

            val request = Request.Builder()
                .url(release.apkDownloadUrl)
                .header("User-Agent", "Onnkiosk-Android")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download body"))
            val totalBytes = if (release.apkSizeBytes > 0) release.apkSizeBytes else body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            onProgress(1.0f)
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstall(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return Result.failure(Exception("Please enable 'Install unknown apps' permission to install the update"))
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        }
    }

    companion object {
        fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
            val cleanRemote = cleanVersion(remoteTag)
            val cleanCurrent = cleanVersion(currentVersion)
            if (cleanRemote.isEmpty() || cleanRemote == cleanCurrent) return false

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
            if (remoteParts.isEmpty()) return false

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        private fun cleanVersion(raw: String): String {
            return raw.trim()
                .removePrefix("android-")
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
        }
    }
}
