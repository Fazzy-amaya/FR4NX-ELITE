package com.fr4nx.elite

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private val tag = "FR4NX"

    private lateinit var menuHeader: LinearLayout
    private lateinit var menuContent: LinearLayout
    private lateinit var menuChevron: ImageView
    private lateinit var detectedLabel: TextView
    private lateinit var launchButton: Button
    private lateinit var overlayButton: Button
    private var menuOpen = true
    private var detected: FreeFireDetector.FFApp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(tag, "D1. install crash guard")
        CrashGuard.install(this)

        Log.d(tag, "D2. super.onCreate")
        super.onCreate(savedInstanceState)

        Log.d(tag, "D3. setContentView")
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        } catch (e: Throwable) {
            Log.e(tag, "setFlags failed", e)
        }
        setContentView(R.layout.activity_dashboard)

        Log.d(tag, "D4. findViewById")
        menuHeader = findViewById(R.id.menu_header)
        menuContent = findViewById(R.id.menu_content)
        menuChevron = findViewById(R.id.menu_chevron)
        detectedLabel = findViewById(R.id.detected_label)
        launchButton = findViewById(R.id.launch_button)
        overlayButton = findViewById(R.id.overlay_button)

        Log.d(tag, "D5. detect")
        try {
            detected = FreeFireDetector.detect(this)
        } catch (e: Throwable) {
            Log.e(tag, "detect failed", e)
            detected = null
        }

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
        menuHeader.setOnClickListener { toggleMenu() }

        Log.d(tag, "D6. wire features")
        try {
            wireFeature(R.id.feat_aimbot, "Aimbot — Precision", false)
            wireFeature(R.id.feat_wallhack, "Wallhack — Full Map", false)
            wireFeature(R.id.feat_esp, "ESP — Player Outline", true)
            wireFeature(R.id.feat_speed, "Speed Boost — x2.4", false)
            wireFeature(R.id.feat_antiban, "Anti-Ban — Spoofed", true)
            wireFeature(R.id.feat_autofire, "Auto-Fire — Trigger", false)
        } catch (e: Throwable) {
            Log.e(tag, "wireFeature failed", e)
        }

        Log.d(tag, "D7. notification permission")
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        } catch (e: Throwable) {
            Log.e(tag, "perm request failed", e)
        }

        Log.d(tag, "D8. done")
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
