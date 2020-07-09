/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.ui.activity.MainActivity

class PlayerNotification(private val service: PlayerService) {
    private val mAppName: String
    private val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val resources: Resources = service.resources
    private var mediaDB: MediaDB? = null

    init {
        mAppName = resources.getString(R.string.app_name)
        mediaDB = MediaDB.getDatabase(service)
    }

    private fun createAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(service, PlayerService::class.java)
        intent.action = action
        return PendingIntent.getService(service, requestCode, intent, 0)
    }

    fun startNotify(playbackStatus: String, currentTag: Long?) {

        var icon = R.drawable.exo_notification_play
        var notificationIcon = R.drawable.exo_notification_play

        var playPauseAction = createAction(PlayerService.ACTION_PAUSE, REQUEST_CODE_PAUSE)

        when (playbackStatus) {
            PlaybackStatus.PAUSED -> {
                icon = R.drawable.exo_notification_play
                notificationIcon = R.drawable.exo_notification_pause
                playPauseAction = createAction(PlayerService.ACTION_PLAY, REQUEST_CODE_PLAY)
            }
            PlaybackStatus.PLAYING -> {
                icon = R.drawable.exo_notification_pause
                notificationIcon = R.drawable.exo_notification_play
                playPauseAction = createAction(PlayerService.ACTION_PAUSE, REQUEST_CODE_PLAY)
            }
            PlaybackStatus.STOPPED -> {
                cancelNotify()
            }
        }

        val stopAction = createAction(PlayerService.ACTION_STOP, REQUEST_CODE_STOP)
        val forwardAction = createAction(PlayerService.ACTION_FORWARD, REQUEST_CODE_FORWARD)
        val rewindAction = createAction(PlayerService.ACTION_REWIND, REQUEST_CODE_REVERSE)

        val intent = Intent(service, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getActivity(service, 0, intent, 0)

        NotificationManagerCompat.from(service).cancel(NOTIFICATION_ID)

        val channelID = "PlayerService"
        val builder = NotificationCompat.Builder(service, channelID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(
                    channelID,
                    "OpenMusix",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        currentTag?.let {
            val current = flow {
                mediaDB?.mediaDataDAO()?.getMediaByID(it)?.let { it1 -> emit(it1) }
            }
            GlobalScope.launch {
                current.collect {
                    val imageURL = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        it.albumID
                    )

                    val projection = arrayOf(
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ARTIST
                    )
                    val selection = "${MediaStore.Audio.AudioColumns._ID} == ${it.fileID}"
                    val query = service.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection, selection, null, null
                    )
                    var detail = ""
                    query.use { x ->
                        val albumColumn =
                            query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                        val artistColumn =
                            query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                        while (x!!.moveToNext()) {
                            val album = query?.getString(albumColumn!!)
                            val artist = query?.getString(artistColumn!!)
                            detail = "$artist - $album"
                        }
                    }

                    val notification = builder.setOngoing(true)
                        .setSmallIcon(notificationIcon)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setContentIntent(pendingIntent)
                        .setStyle(
                            androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(service.getMediaSession().sessionToken)
                                .setShowCancelButton(true)
                                .setShowActionsInCompactView(1, 2, 3)
                                .setCancelButtonIntent(stopAction)

                        )
                        .addAction(R.drawable.exo_notification_previous, "Previous", rewindAction)
                        .addAction(icon, "PlayPause", playPauseAction)
                        .addAction(R.drawable.exo_notification_next, "Forward", forwardAction)
                        .addAction(R.drawable.exo_notification_stop, "Stop", stopAction)
                        .setContentTitle(it.title)
                        .setContentText(detail)
                        .setSubText("${service.exoPlayer?.currentWindowIndex?.plus(1)}/${service.currentPlaylist.size}")
                        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setShowWhen(true)
                        .setNotificationSilent()
                        .setColorized(true)


                    Glide.with(service)
                        .asBitmap()
                        .apply(
                            RequestOptions()
                                .signature(ObjectKey(imageURL))
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                        )
                        .load(imageURL)
                        .into(object : CustomTarget<Bitmap?>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap?>?
                            ) {
                                notification.setLargeIcon(resource)
                                service.startForeground(NOTIFICATION_ID, notification.build())
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)
                                service.startForeground(NOTIFICATION_ID, notification.build())
                            }
                        })
                }
            }
        }
    }

    fun cancelNotify() {
        service.stopForeground(true)
    }


    companion object {

        private val NOTIFICATION_ID = 555

        private val REQUEST_CODE_PAUSE = 1
        private val REQUEST_CODE_PLAY = 2
        private val REQUEST_CODE_STOP = 3
        private val REQUEST_CODE_FORWARD = 4
        private val REQUEST_CODE_REVERSE = 5
    }
}