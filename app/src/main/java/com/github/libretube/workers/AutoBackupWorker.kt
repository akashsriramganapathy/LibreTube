package com.github.libretube.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.util.TextUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun doWork(): Result {
        val context = applicationContext
        
        // 1. Check if enabled
        if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, false)) {
            return Result.success()
        }

        // 2. Get Path
        val pathString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (pathString.isEmpty()) {
            Log.e(TAG(), "Auto backup path not set")
            return Result.failure()
        }

        val treeUri = Uri.parse(pathString)

        // 3. Verify permissions (basic check)
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
             Log.e(TAG(), "Permission missing for backup path: $e")
             // Persistable permission might be lost or not taken yet.
             // We continue and fail at file creation if so.
        }

        val documentDir = DocumentFile.fromTreeUri(context, treeUri)
        if (documentDir == null || !documentDir.canWrite()) {
            Log.e(TAG(), "Cannot write to backup directory")
            return Result.failure()
        }

        // 4. Generate Backup Data
        val options = listOf(
            BackupHelper.BackupOption.WatchHistory,
            BackupHelper.BackupOption.WatchPositions,
            BackupHelper.BackupOption.SearchHistory,
            BackupHelper.BackupOption.LocalSubscriptions,
            BackupHelper.BackupOption.CustomInstances,
            BackupHelper.BackupOption.PlaylistBookmarks,
            BackupHelper.BackupOption.LocalPlaylists,
            BackupHelper.BackupOption.SubscriptionGroups,
            BackupHelper.BackupOption.Preferences
        )
        
        val backupFile = try {
            BackupHelper.createBackup(options)
        } catch (e: Exception) {
            Log.e(TAG(), "Failed to create backup data: $e")
            return Result.retry()
        }

        // 5. Create File
        val timestamp = TextUtils.getFileSafeTimeStampNow()
        val filename = "libretube-autobackup-${timestamp}.json"
        
        val newFile = documentDir.createFile("application/json", filename)
        if (newFile == null) {
             Log.e(TAG(), "Failed to create backup file")
             return Result.failure()
        }

        // 6. Write Data
        return try {
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                JsonHelper.json.encodeToStream(backupFile, outputStream)
            }
            Log.i(TAG(), "Auto backup successful: $filename")
            
            // 7. Prune old backups
            pruneOldBackups(documentDir)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG(), "Error writing backup file: $e")
            Result.failure()
        }
    }

    private fun pruneOldBackups(directory: DocumentFile) {
        val maxKeepString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_MAX_KEEP, "25")
        val maxKeep = maxKeepString.toIntOrNull() ?: 25
        
        val backups = directory.listFiles()
            .filter { it.isFile && it.name?.startsWith("libretube-autobackup-") == true }
            .sortedByDescending { it.name } // libretube-autobackup-YYYY-MM-DD-HH-mm-ss.json sorts well
            
        if (backups.size > maxKeep) {
            val toDelete = backups.drop(maxKeep)
            toDelete.forEach { 
                try {
                    it.delete()
                    Log.d(TAG(), "Deleted old backup: ${it.name}")
                } catch (e: Exception) {
                    Log.e(TAG(), "Failed to delete old backup ${it.name}: $e")
                }
            }
        }
    }
}
