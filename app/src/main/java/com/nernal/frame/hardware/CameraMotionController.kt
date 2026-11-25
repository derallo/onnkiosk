package com.nernal.frame.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * Lightweight Camera2 motion wake detector + on-demand vision snapshot provider.
 * Uses a single low-overhead YUV_420_888 stream and subsamples a 40x30 luma grid every ~750ms.
 * Only converts a full frame to JPEG when [captureSnapshotBase64] is explicitly requested.
 */
class CameraMotionController(
    private val context: Context,
    private val cameraId: String?,
    private val onMotionSample: (motionDetected: Boolean) -> Unit,
    private val onStatusError: (String?) -> Unit = {},
) {
    private val lock = Any()
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    @Volatile
    private var wantRunning = false

    @Volatile
    private var foregroundActive = true

    @Volatile
    private var isOpening = false

    private var prevLumaGrid: ByteArray? = null
    private var lastAnalysisMs = 0L
    private var lastMotionPulseMs = 0L
    private var pendingSnapshot: CompletableDeferred<String?>? = null
    private var temporaryCaptureOnly = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun setForegroundActive(active: Boolean) {
        foregroundActive = active
        if (!active) {
            closeCameraOnly()
        } else if (wantRunning) {
            openIfReady()
        }
    }

    fun setMotionWakeEnabled(enabled: Boolean) {
        wantRunning = enabled
        if (enabled) {
            openIfReady()
        } else {
            onMotionSample(false)
            closeCameraOnly()
        }
    }

    fun release() {
        wantRunning = false
        closeCameraOnly()
        synchronized(lock) {
            handlerThread?.quitSafely()
            handlerThread = null
            handler = null
        }
    }

    suspend fun captureSnapshotBase64(timeoutMs: Long = 3_000L): String? {
        if (cameraId == null || !hasPermission()) return null

        val deferred = CompletableDeferred<String?>()
        val needTempOpen: Boolean
        synchronized(lock) {
            pendingSnapshot?.cancel()
            pendingSnapshot = deferred
            needTempOpen = cameraDevice == null && !isOpening
            if (needTempOpen) {
                temporaryCaptureOnly = true
            }
        }
        if (needTempOpen) {
            openIfReady(forceForSnapshot = true)
        }

        val result = withTimeoutOrNull(timeoutMs) { deferred.await() }
        synchronized(lock) {
            if (pendingSnapshot === deferred) pendingSnapshot = null
            if (temporaryCaptureOnly && !wantRunning) {
                temporaryCaptureOnly = false
                closeCameraOnly()
            }
        }
        return result
    }

    private fun ensureThread(): Handler {
        synchronized(lock) {
            handler?.let { return it }
            val thread = HandlerThread("NernalCameraMotion").apply { start() }
            val h = Handler(thread.looper)
            handlerThread = thread
            handler = h
            return h
        }
    }

    @SuppressLint("MissingPermission")
    private fun openIfReady(forceForSnapshot: Boolean = false) {
        val id = cameraId ?: return
        if (!hasPermission()) return
        if (!foregroundActive && !forceForSnapshot) return
        if (!wantRunning && !forceForSnapshot) return

        synchronized(lock) {
            if (cameraDevice != null || isOpening) return
            isOpening = true
        }

        val bgHandler = ensureThread()
        bgHandler.post {
            try {
                val cm = context.getSystemService(CameraManager::class.java)
                if (cm == null) {
                    finishOpenFailed("CameraManager unavailable")
                    return@post
                }
                cm.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        synchronized(lock) {
                            if ((!wantRunning && !temporaryCaptureOnly) || (!foregroundActive && pendingSnapshot == null)) {
                                camera.close()
                                isOpening = false
                                return
                            }
                            cameraDevice = camera
                            isOpening = false
                        }
                        val reader = try {
                            val size = choosePreviewSize(cm, id)
                            ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
                        } catch (e: Exception) {
                            synchronized(lock) {
                                if (cameraDevice === camera) cameraDevice = null
                            }
                            camera.close()
                            onStatusError(e.message ?: "Unable to configure camera reader")
                            return
                        }
                        reader.setOnImageAvailableListener({ r ->
                            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                            try {
                                handleImage(image)
                            } finally {
                                image.close()
                            }
                        }, bgHandler)
                        synchronized(lock) {
                            if (cameraDevice !== camera) {
                                reader.close()
                                return
                            }
                            imageReader?.close()
                            imageReader = reader
                        }
                        onStatusError(null)
                        startCaptureSession(camera, reader, bgHandler)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        synchronized(lock) {
                            if (cameraDevice === camera) cameraDevice = null
                            isOpening = false
                        }
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        synchronized(lock) {
                            if (cameraDevice === camera) cameraDevice = null
                            isOpening = false
                        }
                        onStatusError("Camera error $error")
                        Log.w("NernalCamera", "Camera2 error=$error")
                    }
                }, bgHandler)
            } catch (e: Exception) {
                finishOpenFailed(e.message ?: "Unable to open camera")
            }
        }
    }

    private fun finishOpenFailed(message: String) {
        synchronized(lock) {
            isOpening = false
            pendingSnapshot?.complete(null)
            pendingSnapshot = null
            temporaryCaptureOnly = false
        }
        onStatusError(message)
        Log.w("NernalCamera", "openIfReady failed: $message")
    }

    private fun startCaptureSession(
        camera: CameraDevice,
        reader: ImageReader,
        bgHandler: Handler,
    ) {
        runCatching {
            val reqBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            @Suppress("DEPRECATION")
            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        synchronized(lock) {
                            if (cameraDevice !== camera) {
                                session.close()
                                return
                            }
                            captureSession = session
                        }
                        runCatching {
                            session.setRepeatingRequest(reqBuilder.build(), null, bgHandler)
                        }.onFailure { e ->
                            Log.w("NernalCamera", "Repeating request failed: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onStatusError("Camera session config failed")
                    }
                },
                bgHandler,
            )
        }.onFailure { e ->
            onStatusError(e.message)
        }
    }

    private fun closeCameraOnly() {
        synchronized(lock) {
            runCatching { captureSession?.close() }
            captureSession = null
            runCatching { cameraDevice?.close() }
            cameraDevice = null
            runCatching { imageReader?.close() }
            imageReader = null
            isOpening = false
            prevLumaGrid = null
        }
    }

    private fun handleImage(image: Image) {
        val snapDeferred = synchronized(lock) {
            val d = pendingSnapshot
            if (d != null) pendingSnapshot = null
            d
        }
        if (snapDeferred != null) {
            val encoded = runCatching { yuv420ImageToJpegBase64(image) }.getOrNull()
            snapDeferred.complete(encoded)
        }

        if (!wantRunning) return

        val now = System.currentTimeMillis()
        if (now - lastAnalysisMs < ANALYSIS_INTERVAL_MS) return
        lastAnalysisMs = now

        val grid = extractLumaGrid(image)
        val prev = prevLumaGrid
        prevLumaGrid = grid

        if (prev != null && prev.size == grid.size) {
            val motion = detectGridMotion(prev, grid)
            if (motion) {
                lastMotionPulseMs = now
                onMotionSample(true)
            } else if (now - lastMotionPulseMs > MOTION_HOLD_MS) {
                onMotionSample(false)
            }
        }
    }

    private fun choosePreviewSize(cm: CameraManager, id: String): Size {
        val map = cm.getCameraCharacteristics(id)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return Size(320, 240)
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888).orEmpty()
        return sizes
            .filter { it.width in 320..640 && it.height in 240..480 }
            .minByOrNull { it.width * it.height }
            ?: sizes.minByOrNull { abs(it.width - 480) + abs(it.height - 360) }
            ?: Size(320, 240)
    }

    companion object {
        private const val ANALYSIS_INTERVAL_MS = 750L
        private const val MOTION_HOLD_MS = 3_000L
        private const val GRID_COLS = 32
        private const val GRID_ROWS = 24

        internal fun extractLumaGrid(image: Image): ByteArray {
            val yPlane = image.planes[0]
            val buf = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val w = image.width.coerceAtLeast(1)
            val h = image.height.coerceAtLeast(1)
            val out = ByteArray(GRID_COLS * GRID_ROWS)
            var idx = 0
            for (r in 0 until GRID_ROWS) {
                val y = (r * h) / GRID_ROWS
                val rowBase = y * rowStride
                for (c in 0 until GRID_COLS) {
                    val x = (c * w) / GRID_COLS
                    val pos = (rowBase + x * pixelStride).coerceIn(0, buf.limit() - 1)
                    out[idx++] = buf.get(pos)
                }
            }
            return out
        }

        internal fun detectGridMotion(prev: ByteArray, curr: ByteArray): Boolean {
            if (prev.size != curr.size || prev.isEmpty()) return false
            var signedSum = 0
            for (i in prev.indices) {
                signedSum += (curr[i].toInt() and 0xFF) - (prev[i].toInt() and 0xFF)
            }
            val meanDrift = signedSum.toFloat() / prev.size
            var adjAbsSum = 0f
            var changedCount = 0
            for (i in prev.indices) {
                val diff = abs(((curr[i].toInt() and 0xFF) - (prev[i].toInt() and 0xFF)) - meanDrift)
                adjAbsSum += diff
                if (diff > 22f) changedCount++
            }
            val mad = adjAbsSum / prev.size
            val ratio = changedCount.toFloat() / prev.size
            return mad > 9.0f || ratio > 0.08f
        }

        private fun yuv420ImageToJpegBase64(image: Image): String? {
            val width = image.width
            val height = image.height
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val nv21 = ByteArray(width * height * 3 / 2)
            val yBuf = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            var pos = 0
            for (row in 0 until height) {
                val rowStart = row * yRowStride
                if (yPixelStride == 1) {
                    yBuf.position(rowStart)
                    yBuf.get(nv21, pos, width)
                    pos += width
                } else {
                    for (col in 0 until width) {
                        nv21[pos++] = yBuf.get(rowStart + col * yPixelStride)
                    }
                }
            }

            val uvHeight = height / 2
            val uvWidth = width / 2
            val uBuf = uPlane.buffer
            val vBuf = vPlane.buffer
            val uRowStride = uPlane.rowStride
            val vRowStride = vPlane.rowStride
            val uPixelStride = uPlane.pixelStride
            val vPixelStride = vPlane.pixelStride

            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val vIdx = row * vRowStride + col * vPixelStride
                    val uIdx = row * uRowStride + col * uPixelStride
                    nv21[pos++] = vBuf.get(vIdx.coerceIn(0, vBuf.limit() - 1))
                    nv21[pos++] = uBuf.get(uIdx.coerceIn(0, uBuf.limit() - 1))
                }
            }

            val out = ByteArrayOutputStream()
            val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            yuv.compressToJpeg(Rect(0, 0, width, height), 80, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    }
}
