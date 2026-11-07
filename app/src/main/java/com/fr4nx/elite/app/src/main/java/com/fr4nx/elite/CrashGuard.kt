package com.fr4nx.elite

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import java.io.File

object CrashGuard {

    private const val TAG = "FR4NX"
    private const val FILE = "last_crash.txt"

    fun install(ctx: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
                File(dir, FILE).writeText(
                    "${e.javaClass.name}: ${e.message}\n\n${e.stackTraceToString()}"
                )
                Log.e(TAG, "CRASH: ${e.message}", e)
            } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }

    fun showIfAny(activity: Activity) {
        try {
            val dir = activity.getExternalFilesDir(null) ?: activity.filesDir
            val f = File(dir, FILE)
            if (!f.exists()) return
            val text = f.readText()
            f.delete()
            AlertDialog.Builder(activity)
                .setTitle("Last crash — screenshot this")
                .setMessage(text)
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show()
        } catch (_: Throwable) {}
    }
}
