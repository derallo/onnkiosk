package com.nernal.frame

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Launches the kiosk app after boot or package update if it isn't already running as the default HOME role. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        ) {
            return
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(launchIntent) }
    }
}
