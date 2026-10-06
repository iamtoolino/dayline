package io.github.iamtoolino.dayline

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

object Access {
    fun usage(context: Context): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    fun ready(context: Context) = usage(context) && Settings.canDrawOverlays(context)
}
