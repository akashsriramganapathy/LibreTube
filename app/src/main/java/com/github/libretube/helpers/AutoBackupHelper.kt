package com.github.libretube.helpers

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.workers.AutoBackupWorker
import java.util.concurrent.TimeUnit

object AutoBackupHelper {
    private const val WORK_TAG = "auto_backup_work"

    fun pruneOldBackups(context: Context) {
        val pathString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (pathString.isEmpty()) return

        val treeUri = try { 
            android.net.Uri.parse(pathString) 
        } catch (e: Exception) { 
            Log.e("AutoBackupHelper", "Failed to parse path URI for pruning: $pathString", e)
            return 
        }

        val documentDir = try {
            DocumentFile.fromTreeUri(context, treeUri)
        } catch (e: Exception) {
            Log.e("AutoBackupHelper", "Failed to get DocumentFile for pruning", e)
            null
        }

        if (documentDir == null || !documentDir.exists()) {
            Log.w("AutoBackupHelper", "Backup directory does not exist or is inaccessible for pruning")
            return
        }

        val maxKeepString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_MAX_KEEP, "25")
        val maxKeep = maxKeepString.toIntOrNull() ?: 25
        
        val allFiles = documentDir.listFiles()
        val autoBackups = allFiles.filter { 
            val name = it.name ?: ""
            it.isFile && (name.startsWith("libretube-autobackup-") || name.startsWith("libretube-backup-")) && name.endsWith(".json")
        }.sortedByDescending { it.name ?: "" }
        
        Log.d("AutoBackupHelper", "Pruning evaluation: total items=${allFiles.size}, matching backups=${autoBackups.size}, max-keep=$maxKeep")

        if (autoBackups.size > maxKeep) {
            val toDelete = autoBackups.drop(maxKeep)
            Log.i("AutoBackupHelper", "Deleting ${toDelete.size} old backups to maintain limit of $maxKeep")
            toDelete.forEach { 
                val name = it.name
                try {
                    if (it.delete()) {
                        Log.d("AutoBackupHelper", "Deleted: $name")
                    } else {
                        Log.e("AutoBackupHelper", "Failed to delete: $name")
                    }
                } catch (e: Exception) {
                    Log.e("AutoBackupHelper", "Error deleting $name: $e")
                }
            }
        } else {
            Log.d("AutoBackupHelper", "No pruning needed (count <= maxKeep)")
        }
    }

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

            // If target time is in the past, move to tomorrow
            // Exception: if it's within the current minute, let it run now (initialDelay = 0).
            if (target.before(now)) {
                val diffMillis = now.timeInMillis - target.timeInMillis
                if (diffMillis > 60000) { // If it's more than 1 minute ago, it's definitely for tomorrow
                    target.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Calculate delay. If target is still before now (same minute case), it results in negative/zero.
            // setInitialDelay handles values <= 0 by running as soon as possible.
            val initialDelayMillis = target.timeInMillis - now.timeInMillis
            Log.d("AutoBackupHelper", "Scheduling backup for $preferredTime (Target: ${target.time}, Delay: ${initialDelayMillis / 1000}s)")
            builder.setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
        }

        val request = builder.build()
        
        // Use CANCEL_AND_REENQUEUE to ensure the new initial delay and interval are applied immediately.
        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }
}
