/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.data.MediaQueue
import xyz.dvnlabs.openmusix.data.QueueDetail
import xyz.dvnlabs.openmusix.service.event.PlayerChange
import xyz.dvnlabs.openmusix.service.event.RxBusEvent
import xyz.dvnlabs.openmusix.util.Converter

/**
 * <b>This class used for API gateway between PlayerService and UI (Activity,Fragment or View)</b>
 */
class OpenMusixAPI(private val context: Context) : LifecycleService() {

    private val mediaDB: MediaDB by inject()
    private val sharedPref =
        context.getSharedPreferences("current", Context.MODE_PRIVATE)

    private val playerControl =
        context.getSharedPreferences("control", Context.MODE_PRIVATE)

    var isServiceBound = false
        private set


    var liveDataChange: MutableLiveData<PlayerChange.CurrentData> = MutableLiveData()
    var liveStateChange: MutableLiveData<PlayerChange.OnPlayerStateChanged> = MutableLiveData()
    var liveTrackChange: MutableLiveData<PlayerChange.OnTrackChange> = MutableLiveData()
    var currentQueue: MutableLiveData<List<PlaylistQueue>> = MutableLiveData()
    var currentRepeatMode: LiveData<Int> =
        repeatMode().asLiveData(lifecycleScope.coroutineContext).distinctUntilChanged()

    private fun repeatMode() = flow {
        while (true) {
            delay(500)
            val mode = playerControl.getInt("REPEAT_MODE", REPEAT_MODE.OFF)
            emit(mode)
        }
    }

    var currentShuffleMode: LiveData<Int> =
        shuffleMode().asLiveData(lifecycleScope.coroutineContext).distinctUntilChanged()

    private fun shuffleMode() = flow {
        while (true) {
            delay(500)
            val mode = playerControl.getInt("SHUFFLE_MODE", SHUFFLE.OFF)
            emit(mode)
        }
    }

    fun bind() {
        api = this
        Log.i(this.javaClass.simpleName, "BINDING")
        Intent(context, PlayerService::class.java).also {
            context.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        isServiceBound = true
    }

    fun unbind() {
        api = null
        Log.i(this.javaClass.simpleName, "UNBINDING")
        context.unbindService(serviceConnection)
        isServiceBound = false
        service = null
    }


    private fun listenPlayerEvent() {
        RxBusEvent.listen(PlayerChange.OnPlayerStateChanged::class.java)
            .distinctUntilChanged()
            .subscribe {
                liveStateChange.postValue(it)
            }
        RxBusEvent.listen(PlayerChange.OnTrackChange::class.java)
            .distinctUntilChanged()
            .subscribe {
                val fileID = it.currentTag as Long?
                fileID?.let {
                    lifecycleScope.launch {
                        mediaDB.mediaDataDAO().addPlayedCount(fileID)
                    }
                }
                liveTrackChange.postValue(it)
            }
        RxBusEvent.listen(PlayerChange.CurrentData::class.java)
            .subscribe {
                with(sharedPref.edit()) {
                    val fileID = it.currentTag as Long?
                    fileID?.let { x ->
                        putLong("file_id", x)
                        commit()
                    }
                }
                currentQueue.postValue(service?.currentPlaylist)
                liveDataChange.postValue(it)
            }
    }

    /**
     * PlayerService pass through section
     *
     * Diagram:
     *
     * UI -> (Send Command through function of API) -> OpenMusixAPI -> PlayerService
     */

    /**
     * API-> playPausePlayer()
     * Used as pass a through to service to play or pause player
     */
    fun playPausePlayer() {
        Log.i(this.javaClass.simpleName, "PlayPause triggered")
        when (service?.isPlaying()) {
            true -> {
                service?.transportControls?.pause()
            }
            false -> {
                service?.transportControls?.play()
            }
            else -> {
                service?.transportControls?.play()
            }
        }
    }

    @Synchronized
    fun playDefault(media: MediaData? = null, position: Long = 0, playWhenReady: Boolean = true) {
        lifecycleScope.launch {
            val mediaData = mediaDB.mediaDataDAO().getMedia()
            val playList = Converter().convertMediaDataToQueue(mediaData)
            var index = 0
            media?.let {
                index = mediaData.indexOfFirst { idx ->
                    idx.fileID == it.fileID
                }
            }
            Log.i(
                this.javaClass.simpleName,
                "Playing with default! Size playlist: ${playList?.size}"
            )
            withContext(Dispatchers.Main) {
                with(sharedPref.edit()) {
                    this.remove("queue")
                    apply()
                }
                playList?.let {
                    //Add to playlist
                    service?.addPlayList(it)
                    //Play The player
                    service?.exoPlayer?.seekTo(index, position)
                    service?.exoPlayer?.playWhenReady = playWhenReady
                }
            }
        }
    }

    fun playNewQueue(name: String = "SYS", medias: List<MediaData>, playWhenReady: Boolean = true) {
        lifecycleScope.launch {
            val playList = Converter().convertMediaDataToQueue(medias)
            Log.i(
                this.javaClass.simpleName,
                "Playing with queue! Size playlist: ${playList?.size}"
            )
            if (name == "SYS") {
                mediaDB.mediaQueueDAO().deleteQueueByName(name)
            }
            val queue = mediaDB.mediaQueueDAO().newQueue(
                MediaQueue(name = name)
            )
            for (i in medias) {
                mediaDB.mediaQueueDetailDAO().newQueueDetail(
                    QueueDetail(queueID = queue, fileID = i.fileID)
                )
            }

            playList?.let {
                withContext(Dispatchers.Main) {
                    //Add to playlist
                    service?.addPlayList(it)
                    //Play The player
                    service?.exoPlayer?.playWhenReady = playWhenReady
                }
            }
            with(sharedPref.edit()) {
                this.putLong("queue", queue)
                apply()
            }
        }
    }


    fun playPlaylist(id: Long, index: Int = 0) {

    }

    fun playerToIndex(index: Int = 0) {
        service?.exoPlayer?.seekTo(index, C.TIME_UNSET)
    }

    /**
     * addPlayLists used to add a list of PlaylistQueue model
     * used to auto queue on PlayerService
     * [playlist] List of PlaylistQueue
     */
    fun addPlaylists(playlist: List<PlaylistQueue>) {
        service?.addPlayList(playlist)
    }

    /**
     * addQueue is to add an item to queue
     *
     * [item] use MediaData model
     *
     *<b>WARNING!!Because this is one time bind,
     * if you adding a queue, please consider to using addPlaylist
     * for more safe in case you use iteration when adding addQueue. Or if you want
     * you can clear queue before adding a queue on clearQueue</b>
     */
    fun addQueue(item: MediaData) {
        service?.addQueue(item)
    }

    fun clearQueue() {

    }

    fun removeQueue() {

    }

    /*Player Control for repeat, shuffle
    * NOT IMPLEMENTED YET
    * */
    fun changeRepeat(mode: Int = REPEAT_MODE.OFF) {
        val exoMode: Int = when (mode) {
            REPEAT_MODE.ONE -> {
                ExoPlayer.REPEAT_MODE_ONE
            }
            REPEAT_MODE.OFF -> {
                ExoPlayer.REPEAT_MODE_OFF
            }
            REPEAT_MODE.ALL -> {
                ExoPlayer.REPEAT_MODE_ALL
            }
            else -> ExoPlayer.REPEAT_MODE_OFF
        }
        with(playerControl.edit()) {
            this.putInt("REPEAT_MODE", exoMode)
            commit()
        }
        service?.exoPlayer?.repeatMode = exoMode
    }

    fun changeRepeatMode() {
        when (currentRepeatMode.value) {
            REPEAT_MODE.DEFAULT -> {
                changeRepeat(REPEAT_MODE.OFF)
            }
            REPEAT_MODE.OFF -> {
                changeRepeat(REPEAT_MODE.ONE)
            }
            REPEAT_MODE.ONE -> {
                changeRepeat(REPEAT_MODE.ALL)
            }
            REPEAT_MODE.ALL -> {
                changeRepeat(REPEAT_MODE.OFF)
            }
            else -> changeRepeat(REPEAT_MODE.OFF)
        }
    }
    //Shuffle

    private fun changeShuffle(mode: Int = SHUFFLE.OFF) {
        val shuffleMode: Boolean = when (mode) {
            SHUFFLE.OFF -> {
                false
            }
            SHUFFLE.ON -> {
                true
            }
            else -> false
        }
        with(playerControl.edit()) {
            this.putInt("SHUFFLE_MODE", mode)
            commit()
        }
        service?.exoPlayer?.shuffleModeEnabled = shuffleMode
    }

    fun changeShuffleMode() {
        when (currentShuffleMode.value) {
            SHUFFLE.OFF -> {
                changeShuffle(SHUFFLE.ON)
            }
            SHUFFLE.ON -> {
                changeShuffle(SHUFFLE.OFF)
            }
            else -> changeShuffle(SHUFFLE.ON)
        }
    }


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            val playerBinder = binder as PlayerService.PlayerBinder
            service = playerBinder.service
            isServiceBound = true
            if (service != null) {
                Log.i(this@OpenMusixAPI.javaClass.simpleName, "BIND OK!")
                listenPlayerEvent()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i(context.packageName, "ServiceConnection::onServiceDisconnected() called")
            service = null
            isServiceBound = false
            if (service == null) {
                api = null
                Log.i(this@OpenMusixAPI.javaClass.simpleName, "Service Disconnected")
            }
        }
    }

    companion object {
        @JvmStatic
        var service: PlayerService? = null
            private set

        @JvmStatic
        var api: OpenMusixAPI? = null
            private set
    }

    object REPEAT_MODE {
        val DEFAULT = -1
        val OFF = 0
        val ONE = 1
        val ALL = 2
    }

    object SHUFFLE {
        val OFF = -1
        val ON = 1
    }
}