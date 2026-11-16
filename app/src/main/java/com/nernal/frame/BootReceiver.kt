package com.nernal.frame

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Launches Nernal after boot if the device hasn't already started it as the default HOME role. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(launchIntent) }
    }
}
