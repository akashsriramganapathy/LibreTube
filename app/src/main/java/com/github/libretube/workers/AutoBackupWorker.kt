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
        Log.d("AutoBackupWorker", "Starting auto backup worker")
        
        // 1. Check if enabled
        if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTO_BACKUP_ENABLED, false)) {
            Log.d("AutoBackupWorker", "Auto backup is disabled in settings")
            return Result.success()
        }

        // 2. Get Path
        val pathString = PreferenceHelper.getString(PreferenceKeys.AUTO_BACKUP_PATH, "")
        if (pathString.isEmpty()) {
            Log.e("AutoBackupWorker", "Auto backup path is empty")
            return Result.failure()
        }

        val treeUri = try {
            Uri.parse(pathString)
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Failed to parse backup path URI: $pathString", e)
            return Result.failure()
        }

        // 3. Verify permissions and access
        val documentDir = try {
             DocumentFile.fromTreeUri(context, treeUri)
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Failed to get DocumentFile from URI", e)
            null
        }

        if (documentDir == null || !documentDir.exists()) {
            Log.e("AutoBackupWorker", "Backup directory does not exist or is inaccessible: $pathString")
            return Result.failure()
        }

        if (!documentDir.canWrite()) {
            Log.e("AutoBackupWorker", "Backup directory is not writeable: ${documentDir.uri}")
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
            Log.e("AutoBackupWorker", "Failed to create backup data object", e)
            return Result.retry()
        }

        // 5. Create File
        val timestamp = TextUtils.getFileSafeTimeStampNow()
        val filename = "libretube-autobackup-${timestamp}.json"
        
        Log.d("AutoBackupWorker", "Creating backup file: $filename")
        val newFile = try {
            documentDir.createFile("application/json", filename)
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Exception while creating backup file", e)
            null
        }

        if (newFile == null) {
             Log.e("AutoBackupWorker", "Failed to create backup file (createFile returned null)")
             return Result.failure()
        }

        // 6. Write Data
        return try {
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                JsonHelper.json.encodeToStream(backupFile, outputStream)
            }
            Log.i("AutoBackupWorker", "Auto backup successful: $filename")
            
            // 7. Prune old backups
            try {
                pruneOldBackups(documentDir)
            } catch (e: Exception) {
                Log.e("AutoBackupWorker", "Pruning failed but backup was successful", e)
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoBackupWorker", "Error writing backup data to file", e)
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
