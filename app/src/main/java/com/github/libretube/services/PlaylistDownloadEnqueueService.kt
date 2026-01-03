package com.github.libretube.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.libretube.LibreTubeApp.Companion.PLAYLIST_DOWNLOAD_ENQUEUE_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadPlaylist
import com.github.libretube.db.obj.DownloadPlaylistVideosCrossRef
import com.github.libretube.enums.NotificationId
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.getWhileDigit
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.parcelable.DownloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.div

class PlaylistDownloadEnqueueService : LifecycleService() {
    private lateinit var nManager: NotificationManager
    
    // Bind to DownloadService to keep it alive during enqueueing
    private var downloadService: DownloadService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? DownloadService.LocalBinder
            downloadService = binder?.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
        }
    }

    private lateinit var playlistId: String
    private lateinit var playlistType: PlaylistType
    private var playlistName: String? = null
    private var maxVideoQuality: Int? = null
    private var maxAudioQuality: Int? = null
    private var audioLanguage: String? = null
    private var captionLanguage: String? = null
    private var amountOfVideos = 0
    private var amountOfVideosDone = 0

    override fun onCreate() {
        super.onCreate()
        
        nManager = getSystemService()!!

        ServiceCompat.startForeground(
            this,
            NotificationId.ENQUEUE_PLAYLIST_DOWNLOAD.id,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
        
        // Bind to DownloadService
        bindService(Intent(this, DownloadService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        try {
            nManager = getSystemService()!!
        } catch (e: Exception) {
             e.printStackTrace()
             stopSelf()
             return START_NOT_STICKY
        }

        playlistId = intent.getStringExtra(IntentData.playlistId) ?: run {
             stopSelf()
             return START_NOT_STICKY
        }
        playlistName = intent.getStringExtra(IntentData.playlistName)
        playlistType = intent.serializableExtra(IntentData.playlistType) ?: run {
             stopSelf()
             return START_NOT_STICKY
        }
        maxVideoQuality = intent.getIntExtra(IntentData.maxVideoQuality, 0).takeIf { it != 0 }
        maxAudioQuality = intent.getIntExtra(IntentData.maxAudioQuality, 0).takeIf { it != 0 }
        captionLanguage = intent.getStringExtra(IntentData.captionLanguage)
        audioLanguage = intent.getStringExtra(IntentData.audioLanguage)

        nManager.notify(NotificationId.ENQUEUE_PLAYLIST_DOWNLOAD.id, buildNotification())

        lifecycleScope.launch(Dispatchers.IO) {
            if (playlistType != PlaylistType.PUBLIC) {
                enqueuePrivatePlaylist()
            } else {
                enqueuePublicPlaylist()
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, PLAYLIST_DOWNLOAD_ENQUEUE_CHANNEL_NAME)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(
                getString(R.string.enqueueing_playlist_download, playlistName ?: "...")
            )
            .setContentText("$amountOfVideosDone / $amountOfVideos")
            .setProgress(amountOfVideos, amountOfVideosDone, false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private suspend fun enqueuePrivatePlaylist() {
        val playlist = try {
            PlaylistsHelper.getPlaylist(playlistId)
        } catch (e: Exception) {
            toastFromMainDispatcher(e.localizedMessage.orEmpty())
            stopSelf()
            return
        }
        amountOfVideos = playlist.videos
        enqueueStreams(playlistId, playlist.relatedStreams)
    }

    private suspend fun enqueuePublicPlaylist() {
        val playlist = try {
            MediaServiceRepository.instance.getPlaylist(playlistId)
        } catch (e: Exception) {
            toastFromMainDispatcher(e.localizedMessage.orEmpty())
            stopSelf()
            return
        }

        val thumbnailPath = getDownloadPath(DownloadHelper.PLAYLIST_THUMBNAIL_DIR, playlistId)
        CoroutineScope(Dispatchers.IO).launch {
            playlist.thumbnailUrl?.let { url ->
                ImageHelper.downloadImage(
                    this@PlaylistDownloadEnqueueService, url, thumbnailPath
                )
            }
        }

        DatabaseHolder.Database.downloadDao().insertPlaylist(
            DownloadPlaylist(
                playlistId = playlistId,
                title = playlist.name.orEmpty(),
                description = playlist.description,
                thumbnailPath = thumbnailPath,
            )
        )

        amountOfVideos = playlist.videos
        enqueueStreams(playlistId, playlist.relatedStreams)

        var nextPage = playlist.nextpage
        // retry each api call once when fetching next pages to increase success chances
        var alreadyRetriedOnce = false

        while (nextPage != null) {
            val playlistPage = runCatching {
                MediaServiceRepository.instance.getPlaylistNextPage(playlistId, nextPage!!)
            }.getOrNull()

            if (playlistPage == null && alreadyRetriedOnce) {
                toastFromMainDispatcher(R.string.unknown_error)
                stopSelf()
                return
            }

            if (playlistPage == null) {
                // retry if previous attempt failed
                alreadyRetriedOnce = true
                continue
            }

            alreadyRetriedOnce = false
            enqueueStreams(playlistId, playlistPage.relatedStreams)
            nextPage = playlistPage.nextpage
        }
    }

    private suspend fun enqueueStreams(playlistId: String, streams: List<StreamItem>) {
        nManager.notify(NotificationId.ENQUEUE_PLAYLIST_DOWNLOAD.id, buildNotification())

        var lastNotifyTime = System.currentTimeMillis()

        // Process streams in parallel chunks to speed up metadata fetching
        streams.chunked(5).forEach { batch -> // Process 5 videos concurrently
            val deferreds = batch.map { stream ->
                CoroutineScope(Dispatchers.IO).async {
                    processSingleStream(stream, playlistId)
                }
            }
            deferreds.awaitAll()
            
            // Notify progress after each batch, rate limited
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotifyTime > 500 || amountOfVideosDone == amountOfVideos) {
                nManager.notify(NotificationId.ENQUEUE_PLAYLIST_DOWNLOAD.id, buildNotification())
                lastNotifyTime = currentTime
            }
        }
    }
    
    private suspend fun processSingleStream(stream: StreamItem, playlistId: String) {
        try {
            val videoId = stream.url!!.toID()

            // link the playlist to the video
            DatabaseHolder.Database.downloadDao().insertPlaylistVideoConnection(
                DownloadPlaylistVideosCrossRef(
                    videoId = videoId,
                    playlistId = playlistId
                )
            )

            // only download videos that have not been downloaded before
            if (!DatabaseHolder.Database.downloadDao().exists(videoId)) {
                val videoInfo = runCatching {
                    MediaServiceRepository.instance.getStreams(videoId)
                }.getOrNull() ?: return

                val videoStream = getStream(videoInfo.videoStreams, maxVideoQuality)
                val audioStream = getStream(videoInfo.audioStreams, maxAudioQuality)

                val downloadData = DownloadData(
                    videoId = videoId,
                    videoFormat = videoStream?.format,
                    videoQuality = videoStream?.quality,
                    audioFormat = audioStream?.format,
                    audioQuality = audioStream?.quality,
                    audioLanguage = audioLanguage.takeIf {
                        videoInfo.audioStreams.any { it.audioTrackLocale == audioLanguage }
                    },
                    subtitleCode = captionLanguage.takeIf {
                         videoInfo.subtitles.any { it.code == captionLanguage }
                    }
                )
                
                // Directly use the bound service if available, effectively keeping it alive
                // But DownloadHelper.startDownloadService handles startIntent
                DownloadHelper.startDownloadService(this@PlaylistDownloadEnqueueService, downloadData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue processing other videos even if one fails
        } finally {
            amountOfVideosDone++
        }
    }

    private fun getStream(streams: List<PipedStream>, maxQuality: Int?): PipedStream? {
        val maxStreamQuality = maxQuality ?: return null

        // sort streams by their quality/bitrate
        val sortedStreams = streams
            .sortedBy { it.quality.getWhileDigit() }

        // return the last item below the maximum quality - or if there's none - the stream with
        // the lowest quality available
        return sortedStreams
            .lastOrNull { it.quality.getWhileDigit()!! <= maxStreamQuality }
            ?: sortedStreams.firstOrNull()
    }

    @Suppress("SameParameterValue")
    private fun getDownloadPath(directory: String, fileName: String): Path {
        return DownloadHelper.getDownloadDir(this, directory) / fileName
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        try {
            unbindService(serviceConnection)
        } catch (e: Exception) {
            // Ignore if not bound
        }
        stopSelf()

        super.onDestroy()
    }
}
