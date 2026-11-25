package com.nernal.frame.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.nernal.frame.MainActivity
import kotlin.math.abs

/**
 * Floating "Return to App" pill shown over Android System / Wi-Fi Settings when
 * SYSTEM_ALERT_WINDOW is granted, so nav-bar-less kiosk tablets can always return.
 */
object ReturnToAppOverlay {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            if (!Settings.canDrawOverlays(appContext)) return@post
            hideInternal()

            val wm = appContext.getSystemService(WindowManager::class.java) ?: return@post
            val density = appContext.resources.displayMetrics.density
            fun dp(value: Int): Int = (value * density).toInt()

            val pillBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(26).toFloat()
                setColor(Color.parseColor("#EE141821"))
                setStroke(dp(2), Color.parseColor("#EAB308"))
            }

            val label = TextView(appContext).apply {
                text = "← Return to App"
                setTextColor(Color.parseColor("#FAF8F5"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                minHeight = dp(52)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                background = pillBg
                elevation = dp(10).toFloat()
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = dp(20)
                y = dp(24)
            }

            val touchSlopPx = dp(12)
            var downRawX = 0f
            var downRawY = 0f
            var startX = 0
            var startY = 0
            var dragged = false

            @SuppressLint("ClickableViewAccessibility")
            label.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = params.x
                        startY = params.y
                        dragged = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downRawX).toInt()
                        val dy = (event.rawY - downRawY).toInt()
                        if (abs(dx) >= touchSlopPx || abs(dy) >= touchSlopPx) {
                            dragged = true
                        }
                        if (dragged) {
                            params.x = (startX - dx).coerceAtLeast(0)
                            params.y = (startY - dy).coerceAtLeast(0)
                            runCatching { wm.updateViewLayout(label, params) }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragged) {
                            returnToApp(appContext)
                        }
                        true
                    }
                    else -> false
                }
            }

            val added = runCatching {
                wm.addView(label, params)
                true
            }.getOrDefault(false)

            if (added) {
                windowManager = wm
                overlayView = label
            }
        }
    }

    fun hide() {
        mainHandler.post { hideInternal() }
    }

    private fun returnToApp(appContext: Context) {
        hideInternal()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        runCatching { appContext.startActivity(intent) }
    }

    private fun hideInternal() {
        val view = overlayView ?: return
        val wm = windowManager
        overlayView = null
        windowManager = null
        if (wm != null) {
            runCatching { wm.removeViewImmediate(view) }
        }
    }
}
