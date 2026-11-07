package com.fr4nx.elite

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object FreeFireDetector {

    data class FFApp(val packageName: String, val label: String)

    private val KNOWN = listOf(
        "com.dts.freefiremax"    to "Free Fire MAX",
        "com.dts.freefireth"     to "Free Fire",
        "com.dts.freefireth.vng" to "Free Fire (VNG)",
        "com.garena.game.kgid"   to "Free Fire ID",
        "com.garena.game.kgth"   to "Free Fire TH"
    )

    fun detect(ctx: Context): FFApp? {
        val pm = ctx.packageManager
        for ((pkg, label) in KNOWN) {
            try {
                pm.getPackageInfo(pkg, 0)
                return FFApp(pkg, label)
            } catch (_: PackageManager.NameNotFoundException) {
                // keep scanning
            }
        }
        return null
    }

    fun launch(ctx: Context, pkg: String): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return true
    }
}
