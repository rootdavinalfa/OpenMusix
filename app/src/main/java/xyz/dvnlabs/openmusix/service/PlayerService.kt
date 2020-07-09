/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service

import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import jp.wasabeef.glide.transformations.BlurTransformation
import org.greenrobot.eventbus.EventBus
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.service.event.PlayerChange
import xyz.dvnlabs.openmusix.service.event.RxBusEvent

object PlaybackStatus {
    const val IDLE = "PlaybackStatus_IDLE"
    const val LOADING = "PlaybackStatus_LOADING"
    const val PLAYING = "PlaybackStatus_PLAYING"
    const val PAUSED = "PlaybackStatus_PAUSED"
    const val STOPPED = "PlaybackStatus_STOPPED"
    const val ERROR = "PlaybackStatus_ERROR"
}

data class PlaylistQueue(
    var tag: Long,
    var mediaData: MediaData
)


class PlayerService : Service(), AudioManager.OnAudioFocusChangeListener, Player.EventListener,
    Runnable {
    companion object {
        private const val PACKAGE_NAME = "xyz.dvnlabs.openmusix"
        const val ACTION_PLAY = "$PACKAGE_NAME.play"
        const val ACTION_PAUSE = "$PACKAGE_NAME.pause"
        const val ACTION_STOP = "$PACKAGE_NAME.stop"
        const val ACTION_FORWARD = "$PACKAGE_NAME.forward"
        const val ACTION_REWIND = "$PACKAGE_NAME.rewind"
    }

    var transportControls: MediaControllerCompat.TransportControls? = null
    private var mediaConnector: MediaSessionConnector? = null
    private var sharedPref: SharedPreferences? = null
    private var audioManager: AudioManager? = null
    private var status: String? = null
    private var streamUrl: String? = null
    var currentPlaylist: List<PlaylistQueue> = emptyList()
    private var handler: Handler? = null
    private var notificationManager: PlayerNotification? = null


    var exoPlayer: SimpleExoPlayer? = null
    private val playerBinder = PlayerBinder()
    private var mediaSession: MediaSessionCompat? = null


    inner class PlayerBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return playerBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (status == PlaybackStatus.IDLE)
            stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        pause()
        notificationManager?.cancelNotify()
        handler?.removeCallbacks(this)
        exoPlayer!!.release()
        exoPlayer!!.removeListener(this)
        mediaSession!!.release()
        super.onDestroy()
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPause() {
            pause()
            super.onPause()
        }

        override fun onStop() {
            stop()
            notificationManager?.cancelNotify()
            super.onStop()
        }

        override fun onPlay() {
            play()
            super.onPlay()
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = this.getSharedPreferences("current", Context.MODE_PRIVATE)
        notificationManager = PlayerNotification(this)
        handler = Handler()
        handler?.postDelayed(this, 500)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSessionCompat(this, javaClass.simpleName)
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession!!.setCallback(mediaSessionCallback)
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
            ).setState(
                PlaybackStateCompat.STATE_PAUSED,
                0,
                1.0f
            )
            .build()
        mediaSession!!.setPlaybackState(stateBuilder)

        val rendererFactory =
            DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        exoPlayer = SimpleExoPlayer.Builder(this, rendererFactory).build()
        exoPlayer!!.addListener(this)
        status = PlaybackStatus.IDLE
        mediaSession!!.isActive = true
        mediaConnector = MediaSessionConnector(mediaSession!!)
        mediaConnector?.setPlayer(exoPlayer)
        transportControls = mediaSession!!.controller.transportControls
    }

    override fun run() {
        EventBus.getDefault().post(
            PlayerChange.CurrentData(
                exoPlayer!!.currentPosition,
                exoPlayer!!.duration,
                exoPlayer!!.currentTag
            )
        )
        handler?.postDelayed(this, 500)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action

        if (TextUtils.isEmpty(action))
            return START_NOT_STICKY

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener {
                    }
                    .build()
            )
        } else {
            audioManager!!.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop()
            return START_NOT_STICKY
        }
        when (action!!) {
            ACTION_PLAY -> {
                transportControls!!.play()
            }
            ACTION_PAUSE -> {
                if (PlaybackStatus.STOPPED === status) {
                    transportControls!!.stop()
                } else {
                    transportControls!!.pause()
                }
            }
            ACTION_STOP -> {
                pause()
                notificationManager?.cancelNotify()
            }
            ACTION_FORWARD -> {
                exoPlayer?.seekTo(exoPlayer!!.nextWindowIndex, C.TIME_UNSET)
                with(sharedPref?.edit()) {
                    this?.putLong("file_id", (exoPlayer!!.currentTag as Long?)!!)
                    this?.commit()
                }
            }
            ACTION_REWIND -> {
                exoPlayer?.seekTo(exoPlayer!!.previousWindowIndex, C.TIME_UNSET)
                with(sharedPref?.edit()) {
                    this?.putLong("file_id", (exoPlayer!!.currentTag as Long?)!!)
                    this?.commit()
                }
            }
        }
        return START_NOT_STICKY
    }


    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                exoPlayer!!.volume = 1f
                //resume()
            }

            AudioManager.AUDIOFOCUS_LOSS -> stop()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying()) pause()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isPlaying())
                exoPlayer!!.volume = 0.1f
        }
    }

    fun getStatus(): String {
        return status!!
    }

    fun play() {
        println("SRV PLAY")
        exoPlayer!!.playWhenReady = true
    }

    fun pause() {
        println("SRV PAUSE")
        exoPlayer!!.playWhenReady = false

        audioManager!!.abandonAudioFocus(this)
    }

    fun resume() {
        if (streamUrl != null)
            play()
    }

    fun stop() {
        exoPlayer!!.stop()
        exoPlayer!!.release()

        audioManager!!.abandonAudioFocus(this)
    }

    fun init(streamUrl: String) {
        this.streamUrl = streamUrl

        val mediaSource = buildMediaSource(Uri.parse(streamUrl))

        exoPlayer!!.prepare(mediaSource)
        exoPlayer!!.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        exoPlayer!!.playWhenReady = true
    }

    fun playOrPause(uri: String?) {
        if (uri != null) {
            Log.e("STREAM-OK:", uri)
            if (streamUrl != null && streamUrl == uri) {
                play()
            } else {
                //Log.e("Service",urli);
                init(uri)

            }
        }

    }

    fun addPlayList(playList: List<PlaylistQueue>) {
        if (playList.isNotEmpty() && playList.size != currentPlaylist.size) {
            var mediaSource: MediaSource? = null
            var mediaProgressive: Array<MediaSource> = emptyArray()
            val mediaMetadataCompat = MediaMetadataCompat.Builder()
            for (i in playList) {
                val imageURL = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    i.mediaData.albumID
                )
                mediaMetadataCompat.putString(
                    MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                    imageURL.toString()
                )
                mediaProgressive += buildMediaSource(Uri.parse(i.mediaData.contentURI), i.tag)
            }
            val metaData = mediaMetadataCompat.build()
            mediaSession?.setMetadata(metaData)
            mediaSource = ConcatenatingMediaSource(*mediaProgressive)
            mediaSource.let {
                exoPlayer!!.prepare(it)
                currentPlaylist = playList
                //exoPlayer!!.playWhenReady = true
            }
        }
    }

    fun isPlaying(): Boolean {
        return this.status == PlaybackStatus.PLAYING
    }

    fun getMediaSession(): MediaSessionCompat {
        return mediaSession!!
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        status = when (playbackState) {
            Player.STATE_BUFFERING -> PlaybackStatus.LOADING
            Player.STATE_ENDED -> {
                notificationManager?.cancelNotify()
                PlaybackStatus.STOPPED
            }
            Player.STATE_IDLE -> PlaybackStatus.IDLE
            Player.STATE_READY -> if (playWhenReady) {
                PlaybackStatus.PLAYING
            } else PlaybackStatus.PAUSED
            else -> PlaybackStatus.IDLE
        }
        updateMetaData()
        RxBusEvent.publish(PlayerChange.OnPlayerStateChanged(status!!))
        notificationManager?.startNotify(status!!, exoPlayer!!.currentTag as Long?)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPlayerError(error: ExoPlaybackException) {
        var errorDetails: String? = ""
        //EventBus.getDefault().post(PlaybackStatus.ERROR);
        when (error.type) {
            ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
            }
            ExoPlaybackException.TYPE_REMOTE -> {
            }
            ExoPlaybackException.TYPE_SOURCE -> {
                errorDetails = error.sourceException.message
            }
            ExoPlaybackException.TYPE_RENDERER -> {
                errorDetails = error.rendererException.message
            }

            ExoPlaybackException.TYPE_UNEXPECTED -> {
                errorDetails = error.unexpectedException.message
            }
        }
    }

    override fun onPositionDiscontinuity(reason: Int) {
        when (reason) {
            Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> {
                with(sharedPref?.edit()) {
                    this?.putLong("file_id", (exoPlayer!!.currentTag as Long?)!!)
                    this?.commit()
                }
                RxBusEvent.publish(
                    PlayerChange.OnTrackChange(
                        exoPlayer!!.currentWindowIndex,
                        exoPlayer!!.currentTag
                    )
                )
                updateMetaData()
                notificationManager?.startNotify(status!!, exoPlayer!!.currentTag as Long?)
            }
            Player.DISCONTINUITY_REASON_AD_INSERTION -> {

            }
            Player.DISCONTINUITY_REASON_INTERNAL -> {

            }
            Player.DISCONTINUITY_REASON_SEEK -> {

            }
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> {

            }
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

    }

    override fun onSeekProcessed() {

    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val userAgent = Util.getUserAgent(applicationContext, "OpenMusix")
        val dataSourceFactory = DefaultDataSourceFactory(
            this, DefaultHttpDataSourceFactory(
                userAgent, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true
            )
        )
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        //val cacheDataSourceFactory = CacheDataSourceFactory(AppController.setVideoCache(), dataSourceFactory)
        return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
            .createMediaSource(uri)
    }

    private fun buildMediaSource(uri: Uri, tag: Long): MediaSource {
        val userAgent = Util.getUserAgent(applicationContext, "OpenMusix")
        val dataSourceFactory = DefaultDataSourceFactory(
            this, DefaultHttpDataSourceFactory(
                userAgent, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true
            )
        )
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
            .setTag(tag)
            .createMediaSource(uri)
    }

    private fun updateMetaData() {
        if (currentPlaylist.isNotEmpty()) {
            val currentSong = currentPlaylist[exoPlayer!!.currentWindowIndex]

            val projection = arrayOf(
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            )
            val selection = "${MediaStore.Audio.AudioColumns._ID} == ${currentSong.tag}"
            val query = this.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null
            )
            var album = ""
            var artist = ""
            query.use { x ->
                val albumColumn =
                    query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val artistColumn =
                    query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                while (x!!.moveToNext()) {
                    album = query!!.getString(albumColumn!!)!!
                    artist = query.getString(artistColumn!!)!!
                }
            }
            val imageURL = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                currentSong.mediaData.albumID
            )
            val metaData = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.mediaData.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_COMPOSER,
                    currentSong.mediaData.composer
                )
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                    currentSong.mediaData.track.toLong()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, currentSong.mediaData.year.toLong())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer!!.duration)
            Glide.with(this)
                .asBitmap()
                .transform(BlurTransformation(5))
                .thumbnail(0.25f)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                )
                .load(imageURL)
                .into(object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        metaData?.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        mediaSession?.setMetadata(metaData.build())
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        mediaSession?.setMetadata(metaData.build())
                    }
                })
        }
    }

}