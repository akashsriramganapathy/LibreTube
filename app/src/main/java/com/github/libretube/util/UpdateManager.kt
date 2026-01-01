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

    private val client = OkHttpClient()

    /**
     * Downloads an APK from the given URL to a temporary file.
     */
    suspend fun downloadApk(url: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
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
                if (!response.isSuccessful) return@withContext false
                
                val body = response.body ?: return@withContext false
                val contentLength = body.contentLength()
                
                body.byteStream().use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytesRead: Long = 0
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                builder.setProgress(100, progress, false)
                                notificationManager.notify(1, builder.build())
                            }
                        }
                    }
                }
            }
            notificationManager.cancel(1)
            true
        } catch (e: Exception) {
            Log.e(TAG(), "Error downloading APK", e)
            builder.setContentText("Download failed")
                .setOngoing(false)
                .setProgress(0, 0, false)
            notificationManager.notify(1, builder.build())
            false
        }
    }

    /**
     * Installs an APK from the given file using the PackageInstaller Session API.
     */
    fun installApk(apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sessionParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        var sessionId = -1
        try {
            sessionId = packageInstaller.createSession(sessionParams)
            val session = packageInstaller.openSession(sessionId)
            
            apkFile.inputStream().use { inputStream ->
                session.openWrite("package", 0, apkFile.length()).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            val intent = Intent(context, UpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            Log.d(TAG(), "Installation session committed: $sessionId")
        } catch (e: Exception) {
            Log.e(TAG(), "Error installing APK via Session API", e)
            if (sessionId != -1) {
                packageInstaller.abandonSession(sessionId)
            }
        }
    }
}

/**
 * Receiver to handle the status of the installation session.
 */
class UpdateReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        
        Log.d("UpdateReceiver", "Received Update Status: $status, Message: $message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                } else {
                    Log.e("UpdateReceiver", "Confirm Intent is null")
                    android.widget.Toast.makeText(context, "Update failed: Confirmation missing", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("UpdateReceiver", "Installation successful")
                android.widget.Toast.makeText(context, "Installation successful", android.widget.Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e("UpdateReceiver", "Installation failed: $status, $message")
                android.widget.Toast.makeText(context, "Installation failed: $message", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
