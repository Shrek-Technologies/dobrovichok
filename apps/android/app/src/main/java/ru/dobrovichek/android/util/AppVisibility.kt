package ru.dobrovichek.android.util

import android.app.ActivityManager
import android.content.Context

object AppVisibility {

    fun isInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val processName = context.packageName
        val processes = manager.runningAppProcesses ?: return false
        return processes.any { proc ->
            proc.processName == processName &&
                proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
}
