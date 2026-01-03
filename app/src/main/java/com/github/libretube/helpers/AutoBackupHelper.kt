package com.github.libretube.helpers

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.workers.AutoBackupWorker
import java.util.concurrent.TimeUnit

object AutoBackupHelper {
    private const val WORK_TAG = "auto_backup_work"

    fun scheduleBackup(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val enabled = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, false)
        if (!enabled) {
            Log.d("AutoBackupHelper", "Auto backup disabled, cancelling work")
            workManager.cancelUniqueWork(WORK_TAG)
            return
        }

        val pathString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (pathString.isEmpty()) {
            Log.w("AutoBackupHelper", "Auto backup enabled but path is not set, cancelling work")
            workManager.cancelUniqueWork(WORK_TAG)
            return
        }

        var intervalHours = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_INTERVAL, "24").toLongOrNull() ?: 24L
        if (intervalHours < 1) intervalHours = 24

        val builder = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalHours, TimeUnit.HOURS)
            .addTag(WORK_TAG)

        // Calculate initial delay if interval is daily or more to align with preferred time
        if (intervalHours >= 24) {
            val now = java.util.Calendar.getInstance()
            val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(java.util.Calendar.MINUTE)
            val defaultTime = String.format("%02d:%02d", currentHour, currentMinute)

            val preferredTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, defaultTime)
            val parts = preferredTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: currentHour
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: currentMinute

            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis
            Log.d("AutoBackupHelper", "Scheduling backup for $preferredTime (initial delay: ${initialDelayMillis / 1000}s)")
            builder.setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
        }

        val request = builder.build()
        
        // Use KEEP or UPDATE? If we want to CHANGE the time/interval, we must use UPDATE.
        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
