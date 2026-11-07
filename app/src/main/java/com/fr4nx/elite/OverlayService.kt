package com.fr4nx.elite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var root: View? = null
    private lateinit var chip: View
    private lateinit var panel: LinearLayout
    private var collapsed = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        attachOverlay()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "fr4nx_overlay",
                "FR4NX Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, "fr4nx_overlay")
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return builder
            .setContentTitle("FR4NX ELITE")
            .setContentText("Mod overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun attachOverlay() {
        if (root != null) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_menu, null)
        chip = view.findViewById(R.id.overlay_chip)
        panel = view.findViewById(R.id.overlay_panel)
        root = view

        var lastX = 0
        var lastY = 0
        var downRawX = 0f
        var downRawY = 0f
        var dragged = false

        chip.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = params.x
                    lastY = params.y
                    downRawX = e.rawX
                    downRawY = e.rawY
                    dragged = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downRawX).toInt()
                    val dy = (e.rawY - downRawY).toInt()
                    if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) dragged = true
                    params.x = lastX + dx
                    params.y = lastY + dy
                    wm.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) togglePanel()
                    true
                }
                else -> false
            }
        }

        wm.addView(view, params)
    }

    private fun togglePanel() {
        collapsed = !collapsed
        if (!collapsed) {
            panel.visibility = View.VISIBLE
            panel.alpha = 0f
            panel.animate().alpha(1f).setDuration(160).start()
        } else {
            panel.animate().alpha(0f).setDuration(120).withEndAction {
                panel.visibility = View.GONE
            }.start()
        }
    }

    override fun onDestroy() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        super.onDestroy()
    }
}
