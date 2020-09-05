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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
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
import com.google.android.exoplayer2.util.Util
import jp.wasabeef.glide.transformations.BlurTransformation
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
    private var audioAttributes: AudioAttributes? = null
    private var status: String? = null
    private var streamUrl: String? = null
    var currentPlaylist: MutableList<PlaylistQueue> = arrayListOf()
    var queueMediaSource: ConcatenatingMediaSource? = null
    private var handler: Handler? = null
    private var notificationManager: PlayerNotification? = null

    private val intentFilterNoisy = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val audioOutputDetector = AudioOutputDetector()

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
        unregisterReceiver(audioOutputDetector)
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

        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        //Set AudioFocus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        audioAttributes!!
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
            )
        } else {
            audioManager!!.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        val rendererFactory =
            DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        exoPlayer = SimpleExoPlayer.Builder(this, rendererFactory).build()
        exoPlayer!!.addListener(this)
        status = PlaybackStatus.IDLE
        mediaSession!!.isActive = true
        mediaConnector = MediaSessionConnector(mediaSession!!)
        mediaConnector?.setPlayer(exoPlayer)
        transportControls = mediaSession!!.controller.transportControls
        registerReceiver(audioOutputDetector, intentFilterNoisy)
    }

    override fun run() {
        with(sharedPref?.edit()) {
            this?.putLong("position", exoPlayer!!.currentPosition)
            this?.apply()
        }
        RxBusEvent.publish(
            PlayerChange.CurrentData(
                exoPlayer!!.currentPosition,
                exoPlayer!!.duration,
                exoPlayer!!.currentTag
            )
        )
        handler?.postDelayed(this, 500)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(this.javaClass.simpleName, "OnStartCommand")
        val action = intent!!.action

        if (TextUtils.isEmpty(action))
            return START_NOT_STICKY
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
                println("NEXT: ${exoPlayer?.nextWindowIndex} CURRENT: ${exoPlayer?.currentWindowIndex}")
                exoPlayer?.seekTo(exoPlayer!!.nextWindowIndex, 0)
                with(sharedPref?.edit()) {
                    this?.putLong("file_id", (exoPlayer!!.currentTag as Long?)!!)
                    this?.commit()
                }
            }
            ACTION_REWIND -> {
                exoPlayer?.seekTo(exoPlayer!!.previousWindowIndex, 0)
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
                exoPlayer?.playWhenReady = true
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                if (isPlaying()) pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying())
                    exoPlayer!!.volume = 0.4f
            }
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
        Log.i(this.javaClass.simpleName, "Player Stopped")
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

    fun addPlayList(playList: List<PlaylistQueue>) {
        if (playList.isNotEmpty() && playList != currentPlaylist) {
            val mediaSource: MediaSource?
            var mediaProgressive: Array<MediaSource> = emptyArray()
            for (i in playList) {
                mediaProgressive += buildMediaSource(Uri.parse(i.mediaData.contentURI), i.tag)
            }
            mediaSource = ConcatenatingMediaSource(*mediaProgressive)
            mediaSource.let {
                exoPlayer!!.prepare(it)
                currentPlaylist = playList.toMutableList()
                //exoPlayer!!.playWhenReady = true
            }
        }
    }

    fun addQueue(media: MediaData) {
        val mediaSource: MediaSource?
        var mediaProgressive: Array<MediaSource> = emptyArray()
        mediaProgressive += buildMediaSource(Uri.parse(media.contentURI), media.fileID)
        mediaSource = ConcatenatingMediaSource(*mediaProgressive)
        mediaSource.let {
            exoPlayer!!.prepare(it)
            currentPlaylist.add(PlaylistQueue(media.fileID, media))
            //exoPlayer!!.playWhenReady = true
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
                val repeatMode = OpenMusixAPI.api?.currentRepeatMode?.value
                repeatMode?.let {
                    OpenMusixAPI.api?.changeRepeat(repeatMode)
                }
                RxBusEvent.publish(
                    PlayerChange.OnTrackChange(
                        exoPlayer!!.currentWindowIndex,
                        exoPlayer!!.currentTag
                    )
                )
                PlaybackStatus.PLAYING
            } else PlaybackStatus.PAUSED
            else -> PlaybackStatus.IDLE
        }
        updateMetaData()
        RxBusEvent.publish(PlayerChange.OnPlayerStateChanged(status!!))
        notificationManager?.startNotify(status!!, exoPlayer!!.currentTag as Long?)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        exoPlayer?.repeatMode = repeatMode
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = shuffleModeEnabled
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
            try {
                val currentSong = currentPlaylist[exoPlayer!!.currentWindowIndex]

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, Uri.parse(currentSong.mediaData.contentURI))
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val imageURL = retriever.embeddedPicture

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
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_YEAR,
                        currentSong.mediaData.year.toLong()
                    )
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
                            metaData?.putBitmap(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                                resource
                            )
                            mediaSession?.setMetadata(metaData.build())
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            mediaSession?.setMetadata(metaData.build())
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}