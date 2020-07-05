package xyz.dvnlabs.openmusix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.NotificationTarget
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.ui.activity.MainActivity

class PlayerNotification(private val service: PlayerService) {
    private val mAppName: String
    private val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val resources: Resources = service.resources
    private var mediaDB: MediaDataDB? = null

    init {
        mAppName = resources.getString(R.string.app_name)
        mediaDB = MediaDataDB.getDatabase(service)
    }

    private fun createAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(service, PlayerService::class.java)
        intent.action = action
        return PendingIntent.getService(service, requestCode, intent, 0)
    }

    fun startNotify(playbackStatus: String, currentTag: Long?) {

        var icon = R.drawable.exo_notification_play

        var playPauseAction = createAction(PlayerService.ACTION_PAUSE, REQUEST_CODE_PAUSE)

        when (playbackStatus) {
            PlaybackStatus.PAUSED -> {
                icon = R.drawable.exo_notification_play
                playPauseAction = createAction(PlayerService.ACTION_PLAY, REQUEST_CODE_PLAY)
            }
            PlaybackStatus.PLAYING -> {
                icon = R.drawable.exo_notification_pause
                playPauseAction = createAction(PlayerService.ACTION_PAUSE, REQUEST_CODE_PLAY)
            }
            PlaybackStatus.STOPPED -> {
                cancelNotify()
            }
        }

        val stopAction = createAction(PlayerService.ACTION_STOP, REQUEST_CODE_STOP)
        val forwardAction = createAction(PlayerService.ACTION_FORWARD, REQUEST_CODE_FORWARD)

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
                    "Playback Control",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationLayout =
            RemoteViews("xyz.dvnlabs.openmusix", R.layout.notification_small)
        val notificationLayoutLarge =
            RemoteViews("xyz.dvnlabs.openmusix", R.layout.notification_large)
        val notification = builder.setOngoing(true)
            .setSmallIcon(icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(mAppName)
            .setContentText("We are testing notification")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                    .setMediaSession(service.getMediaSession().sessionToken)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopAction)
            )
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setCustomContentView(notificationLayout)
            .setNotificationSilent()
            .setColorized(true)
            .setCustomBigContentView(notificationLayoutLarge)
            .build()
        GlobalScope.launch {
            currentTag?.let {
                val mediaCurrent = mediaDB?.mediaDataDAO()?.getMediaByID(it)
                val imageURL = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    mediaCurrent!!.albumID
                )
                val notificationTarget = NotificationTarget(
                    service.applicationContext,
                    R.id.notification_image,
                    notificationLayout,
                    notification,
                    NOTIFICATION_ID
                )
                withContext(Dispatchers.Main) {
                    Glide.with(service)
                        .asBitmap()
                        .apply(
                            RequestOptions()
                                .override(600, 600)
                                .skipMemoryCache(true)
                                .signature(ObjectKey(System.currentTimeMillis()))
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                        )
                        .load(imageURL)
                        .into(notificationTarget)
                    notificationLayout.setImageViewResource(
                        R.id.notification_image,
                        R.drawable.ic_song
                    )
                    notificationLayout.setImageViewResource(R.id.notification_play_pause, icon)
                    notificationLayout.setImageViewResource(
                        R.id.notification_forward,
                        R.drawable.exo_notification_fastforward
                    )
                    notificationLayout.setImageViewResource(
                        R.id.notification_stop,
                        R.drawable.exo_notification_stop
                    )
                    val projection = arrayOf(
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ARTIST
                    )
                    val selection = "${MediaStore.Audio.AudioColumns._ID} == ${mediaCurrent.fileID}"
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
                    notificationLayout.setTextViewText(R.id.notification_title, mediaCurrent.title)
                    notificationLayout.setTextViewText(R.id.notification_detail, detail)
                    notificationLayout.setOnClickPendingIntent(
                        R.id.notification_play_pause,
                        playPauseAction
                    )
                    notificationLayout.setOnClickPendingIntent(R.id.notification_stop, stopAction)
                    notificationLayout.setOnClickPendingIntent(
                        R.id.notification_forward,
                        forwardAction
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    service.startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    fun cancelNotify() {
        notificationManager.cancel(NOTIFICATION_ID)
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