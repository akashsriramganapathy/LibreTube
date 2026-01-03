package com.github.libretube.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import com.github.libretube.util.TextUtils
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.TimeUnit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.github.libretube.logger.FileLogger

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, false)) {
            return@withContext Result.success()
        }

        val backupPath = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (backupPath.isEmpty()) {
            FileLogger.e(TAG, "Auto Backup skipped: Path not set")
            return@withContext Result.failure()
        }

        val uri = Uri.parse(backupPath)
        val tree = DocumentFile.fromTreeUri(applicationContext, uri)
        if (tree == null || !tree.canWrite()) {
            FileLogger.e(TAG, "Cannot write to backup location: $backupPath")
            return@withContext Result.failure()
        }

        FileLogger.i(TAG, "Starting Auto Backup...")
        val backupFile = BackupFile()
        
        // Populate BackupFile with all data (Auto Backup implies full backup)
        val watchHistory = Database.watchHistoryDao().getAll()
        FileLogger.d(TAG, "Fetched ${watchHistory.size} watch history items")
        backupFile.watchHistory = watchHistory

        backupFile.watchPositions = Database.watchPositionDao().getAll()
        backupFile.searchHistory = Database.searchHistoryDao().getAll()
        backupFile.subscriptions = Database.localSubscriptionDao().getAll()
        backupFile.playlistBookmarks = Database.playlistBookmarkDao().getAll()
        backupFile.localPlaylists = Database.localPlaylistsDao().getAll()
        backupFile.groups = Database.subscriptionGroupsDao().getAll()
        
        backupFile.preferences = PreferenceHelper.dataStore.getAll().map { (key, value) ->
            val jsonValue = when {
                value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> JsonPrimitive(value.toBoolean())
                value.toIntOrNull() != null -> JsonPrimitive(value.toInt())
                value.toLongOrNull() != null -> JsonPrimitive(value.toLong())
                value.toFloatOrNull()?.run { if (!isInfinite() && !isNaN()) JsonPrimitive(this) else null } != null -> JsonPrimitive(value.toFloat())
                else -> JsonPrimitive(value)
            }
            PreferenceItem(key, jsonValue)
        }

        val timestamp = TextUtils.getFileSafeTimeStampNow()
        val fileName = "libretube-auto-backup-$timestamp.json"

        try {
            // Serialize to string first to match Manual Backup behavior
            val jsonString = kotlinx.serialization.json.Json.encodeToString(backupFile)
            FileLogger.d(TAG, "Serialized backup size: ${jsonString.length} chars")

            val file = tree.createFile("application/json", fileName)
            if (file == null) {
                FileLogger.e(TAG, "Failed to create backup file")
                return@withContext Result.failure()
            }

            applicationContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            
            FileLogger.i(TAG, "Backup written successfully to $fileName")

            // Prune old backups
            pruneBackups(tree)
            
        } catch (e: Exception) {
            FileLogger.e(TAG, "Error during auto backup", e)
            return@withContext Result.failure()
        }

        return@withContext Result.success()
    }

    private fun pruneBackups(tree: DocumentFile) {
        val maxFilesStr = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_MAX_FILES, "3")
        val maxFiles = maxFilesStr.toIntOrNull() ?: 3
        
        val files = tree.listFiles().filter { 
            it.name?.startsWith("libretube-auto-backup-") == true && it.name?.endsWith(".json") == true 
        }.sortedByDescending { it.lastModified() } // Newest first

        if (files.size > maxFiles) {
            val toDelete = files.drop(maxFiles)
            FileLogger.d(TAG, "Pruning ${toDelete.size} old backups")
            toDelete.forEach { it.delete() }
        }
    }

    companion object {
        const val TAG = "AutoBackupWorker"
        const val WORK_NAME = "auto_backup_work"
        const val AUTO_BACKUP_MAX_FILES_DEFAULT = 3

        fun enqueueWork(context: Context, existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE) {
            val targetTime = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_TIME, "02:00")
            val parts = targetTime.split(":")
            val targetHour = parts.getOrElse(0) { "02" }.toInt()
            val targetMinute = parts.getOrElse(1) { "00" }.toInt()

            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, targetMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis
            
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            FileLogger.d(TAG, "Scheduling Auto Backup for ${dateFormat.format(target.time)} (in ${initialDelay / 1000}s)")

            val cleanupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
             .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                existingPeriodicWorkPolicy,
                cleanupRequest
            )
        }

        fun runNow(context: Context) {
            FileLogger.i(TAG, "Manual AutoBackup Triggered")
            val request = androidx.work.OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
