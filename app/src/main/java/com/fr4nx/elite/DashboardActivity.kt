package com.fr4nx.elite

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class DashboardActivity : AppCompatActivity() {

    private lateinit var floatingMenu: LinearLayout
    private lateinit var menuHeader: LinearLayout
    private lateinit var menuContent: LinearLayout
    private lateinit var menuChevron: ImageView
    private lateinit var detectedLabel: TextView
    private lateinit var launchButton: Button
    private lateinit var overlayButton: Button
    private var menuOpen = true
    private var detected: FreeFireDetector.FFApp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_dashboard)

        floatingMenu = findViewById(R.id.floating_menu)
        menuHeader = findViewById(R.id.menu_header)
        menuContent = findViewById(R.id.menu_content)
        menuChevron = findViewById(R.id.menu_chevron)
        detectedLabel = findViewById(R.id.detected_label)
        launchButton = findViewById(R.id.launch_button)
        overlayButton = findViewById(R.id.overlay_button)

        detected = FreeFireDetector.detect(this)
        if (detected != null) {
            detectedLabel.text = "TARGET: ${detected!!.label}"
            detectedLabel.setTextColor(0xFF00FFB2.toInt())
            launchButton.isEnabled = true
        } else {
            detectedLabel.text = "TARGET: not installed"
            detectedLabel.setTextColor(0xFF8A93A6.toInt())
            launchButton.isEnabled = false
            launchButton.alpha = 0.5f
        }

        launchButton.setOnClickListener {
            detected?.let {
                val ok = FreeFireDetector.launch(this, it.packageName)
                if (!ok) Toast.makeText(this, "Launch intent not found", Toast.LENGTH_SHORT).show()
            }
        }

        overlayButton.setOnClickListener { enableOverlay() }

        setupDraggableMenu()

        wireFeature(R.id.feat_aimbot, "Aimbot — Precision", false)
        wireFeature(R.id.feat_wallhack, "Wallhack — Full Map", false)
        wireFeature(R.id.feat_esp, "ESP — Player Outline", true)
        wireFeature(R.id.feat_speed, "Speed Boost — x2.4", false)
        wireFeature(R.id.feat_antiban, "Anti-Ban — Spoofed", true)
        wireFeature(R.id.feat_autofire, "Auto-Fire — Trigger", false)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun setupDraggableMenu() {
        var dX = 0f
        var dY = 0f
        var isDragging = false
        var startX = 0f
        var startY = 0f

        menuHeader.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    startX = event.rawX
                    startY = event.rawY
                    dX = floatingMenu.x - event.rawX
                    dY = floatingMenu.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - startX) > 10 || abs(event.rawY - startY) > 10) {
                        isDragging = true
                        floatingMenu.x = event.rawX + dX
                        floatingMenu.y = event.rawY + dY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun wireFeature(cardId: Int, name: String, defaultOn: Boolean) {
        val card = findViewById<View>(cardId) ?: return
        val label = card.findViewById<TextView>(R.id.feature_label) ?: return
        val state = card.findViewById<TextView>(R.id.feature_state) ?: return
        label.text = name
        var on = defaultOn
        state.text = if (on) "ON" else "OFF"
        state.setTextColor(if (on) 0xFF00FFB2.toInt() else 0xFF8A93A6.toInt())
        card.alpha = if (on) 1f else 0.72f
        card.setOnClickListener {
            on = !on
            state.text = if (on) "ON" else "OFF"
            state.setTextColor(if (on) 0xFF00FFB2.toInt() else 0xFF8A93A6.toInt())
            card.alpha = if (on) 1f else 0.72f
            card.animate().scaleX(1.02f).scaleY(1.02f).setDuration(70).withEndAction {
                card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
        }
    }

    private fun toggleMenu() {
        if (menuOpen) {
            menuContent.animate().alpha(0f).translationY(-18f).setDuration(200).withEndAction {
                menuContent.visibility = View.GONE
            }.start()
        } else {
            menuContent.visibility = View.VISIBLE
            menuContent.alpha = 0f
            menuContent.translationY = -18f
            menuContent.animate().alpha(1f).translationY(0f).setDuration(220).start()
        }
        menuOpen = !menuOpen
        menuChevron.animate().rotation(if (menuOpen) 180f else 0f).setDuration(220).start()
    }

    private fun enableOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            Toast.makeText(this, "Grant overlay permission, then tap again", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            startForegroundService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "FR4NX overlay active", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Overlay failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
