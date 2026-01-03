package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.github.libretube.extensions.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * UpdateManager handles downloading and installing APKs using the Android Session API.
 */
class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Downloads an APK from the given URL to a temporary file.
     */
    suspend fun downloadApk(url: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        com.github.libretube.logger.FileLogger.d("UpdateManager", "Starting download: $url -> ${outputFile.path}")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "update_download_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             val channel = android.app.NotificationChannel(
                channelId,
                "Update Download",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Update")
            .setOngoing(true)
            .setProgress(100, 0, false)
        
        notificationManager.notify(1, builder.build())

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    com.github.libretube.logger.FileLogger.e("UpdateManager", "Download failed: HTTP ${response.code}")
                    return@withContext false
                }
                
                val body = response.body ?: return@withContext false
                val contentLength = body.contentLength()
                
                body.byteStream().use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytesRead: Long = 0
                        var lastProgress = 0
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                if (progress > lastProgress) {
                                    lastProgress = progress
                                    builder.setProgress(100, progress, false)
                                    notificationManager.notify(1, builder.build())
                                }
                            }
                        }
                    }
                }
            }
            notificationManager.cancel(1)
            com.github.libretube.logger.FileLogger.d("UpdateManager", "Download finished successfully")
            true
        } catch (e: Exception) {
            com.github.libretube.logger.FileLogger.e("UpdateManager", "Error downloading APK", e)
            Log.e(TAG(), "Error downloading APK", e)
            builder.setContentText("Download failed")
                .setOngoing(false)
                .setProgress(0, 0, false)
            notificationManager.notify(1, builder.build())
            false
        }
    }
}

