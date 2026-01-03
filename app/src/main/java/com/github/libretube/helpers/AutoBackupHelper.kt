package com.github.libretube.helpers

import android.content.Context
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
            workManager.cancelUniqueWork(WORK_TAG)
            return
        }

        // Get interval in hours? Or days?
        // Let's assume the preference stores HOURS for simplicity, or an index mapped to hours.
        // User said "schedule time".
        // If we want specific *time* (e.g. 2:00 AM), we need to calculate initial delay.
        // For now, let's implement interval-based (e.g. Daily = 24h).
        // Let's interpret AUTO_BACKUP_INTERVAL as "Hours".
        var intervalHours = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_INTERVAL, "24").toLongOrNull() ?: 24L
        if (intervalHours < 1) intervalHours = 24

        val builder = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalHours, TimeUnit.HOURS)
            .addTag(WORK_TAG)

        // Calculate initial delay if interval is daily or more to align with preferred time
        if (intervalHours >= 24) {
            val preferredTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, "02:00")
            val parts = preferredTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 2
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val now = java.util.Calendar.getInstance()
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
            builder.setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
        }

        val request = builder.build()
        
        // Use UPDATE so if parameters change (like interval or time), it reschedules.
        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
