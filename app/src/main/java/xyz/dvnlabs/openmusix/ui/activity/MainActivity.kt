package xyz.dvnlabs.openmusix.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.animation.AnimationUtils
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.C
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.app.AppSingleton
import xyz.dvnlabs.openmusix.base.BaseActivity
import xyz.dvnlabs.openmusix.data.MediaDataDB
import xyz.dvnlabs.openmusix.databinding.ActivityMainBinding
import xyz.dvnlabs.openmusix.event.PlayerData
import xyz.dvnlabs.openmusix.service.PlayerManager
import xyz.dvnlabs.openmusix.service.PlaylistQueue
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import xyz.dvnlabs.openmusix.util.PermissionData
import xyz.dvnlabs.openmusix.util.PermissionHelper
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemReselectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private val listVM: ListViewModel by viewModel()
    private val mediaDB: MediaDataDB by inject()
    private var playerManager: PlayerManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)

        val check = arrayOf(
            PermissionData(Manifest.permission.READ_EXTERNAL_STORAGE, 22),
            PermissionData(Manifest.permission.WRITE_EXTERNAL_STORAGE, 22)
        )
        PermissionHelper.checkPermission(this, check)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigationHost) as NavHostFragment
        binding.playerLayout.playerExpand.setOnClickListener {
            navHostFragment.navController.navigate(R.id.fragmentPlayer)
            unSelectBottomNav()
        }
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragmentPlayer -> {
                    val layoutParams =
                        binding.playerLayout.root.layoutParams as CoordinatorLayout.LayoutParams
                    val bottomViewNavBehavior =
                        layoutParams.behavior as HideBottomViewOnScrollBehavior
                    bottomViewNavBehavior.slideUp(binding.playerLayout.root)
                    unSelectBottomNav()
                }
                else -> {
                    binding.playerLayout.playerExpand.isExpanded = true
                    binding.playerLayout.bottomNav.menu.findItem(destination.id).isChecked = true
                }
            }
        }
        val sharedPref = this.getSharedPreferences("current", Context.MODE_PRIVATE)
        var fileID = -1L
        Observable.create<Long> { emitter: ObservableEmitter<Long>? ->
            Schedulers.newThread().schedulePeriodicallyDirect({
                val fileID = sharedPref.getLong("file_id", -1)
                emitter?.onNext(fileID)
            }, 0, 500, TimeUnit.MILLISECONDS)
        }.observeOn(AndroidSchedulers.mainThread()).subscribe {
            fileID = it
            listVM.changeFileID(it)
        }
        binding.playerLayout.bottomNav.setOnNavigationItemSelectedListener(this)


        playerManager = PlayerManager(this)
        if (PlayerManager.service == null) {
            playerManager?.bind()
            listVM.currentFileID.observe(this, Observer {
                lifecycleScope.launch {
                    currentPlaying(it)
                }
            })
        }
        binding.playerLayout.playerButton.setOnClickListener {
            lifecycleScope.launch {
                currentPlaying(fileID)
            }
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.click_anim_1))
            when (PlayerManager.service?.isPlaying()) {
                true -> {
                    PlayerManager.service?.transportControls?.pause()
                }
                false -> {
                    PlayerManager.service?.transportControls?.play()
                }
                else -> {
                    PlayerManager.service?.transportControls?.play()
                }
            }
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }


    private suspend fun currentPlaying(fileID: Long) {
        if (fileID == -1L || fileID == 0L) {
            withContext(Dispatchers.Main) {
                binding.playerLayout.playerExpand.isExpanded = false
            }
        } else {
            withContext(Dispatchers.IO) {
                val mediaData = mediaDB.mediaDataDAO().getMedia()

                val current = mediaData.singleOrNull { x ->
                    x.fileID == fileID
                }
                val nowIndex = mediaData.indexOf(current)

                val projection = arrayOf(
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST
                )
                val selection = "${MediaStore.Audio.AudioColumns._ID} == ${current!!.fileID}"
                val query = this@MainActivity.contentResolver.query(
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
                val imageURL = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    current.albumID
                )


                withContext(Dispatchers.Main) {
                    binding.playerLayout.playerTitle.text = current.title
                    binding.playerLayout.playerDetail.text = detail
                    Glide.with(this@MainActivity)
                        .applyDefaultRequestOptions(
                            RequestOptions()
                                .placeholder(R.drawable.ic_song)
                                .error(R.drawable.ic_song)
                        )
                        .load(imageURL).transform(RoundedCorners(10))
                        .transition(
                            DrawableTransitionOptions()
                                .crossFade()
                        ).apply(
                            RequestOptions()
                                .override(600, 600)
                        ).into(binding.playerLayout.playerArt)

                    val playList = arrayListOf<PlaylistQueue>()
                    for (a in mediaData) {
                        playList.add(
                            PlaylistQueue(
                                a.contentURI,
                                a.fileID,
                                a.albumID,
                                a.artistID,
                                a.title
                            )
                        )
                    }
                    PlayerManager.service?.addPlayList(playList)
                    val tag = PlayerManager.service?.exoPlayer?.currentTag
                    if (tag != fileID) {
                        PlayerManager.service?.exoPlayer?.seekTo(nowIndex, C.TIME_UNSET)
                    }
                    if (PlayerManager.service?.exoPlayer?.playbackState != PlaybackState.STATE_PLAYING) {
                        PlayerManager.service?.exoPlayer?.seekTo(nowIndex, C.TIME_UNSET)
                    }
                }
            }
        }

    }

    @SuppressLint("RestrictedApi")
    private fun unSelectBottomNav() {
        binding.playerLayout.playerExpand.isExpanded = false
        val menu = binding.playerLayout.bottomNav.menu
        for (i in 0 until menu.size()) {
            (menu.getItem(i) as? MenuItemImpl)?.let {
                it.isExclusiveCheckable = false
                it.isChecked = false
                it.isExclusiveCheckable = true
            }
        }
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        binding.playerLayout.playerExpand.isExpanded = true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.playerLayout.playerExpand.isExpanded = true
        val navController = findNavController(R.id.navigationHost)
        return when (item.itemId) {
            R.id.fragmentLibrary -> {
                navController.navigate(R.id.fragmentLibrary)
                true
            }
            R.id.fragmentSetting -> {
                navController.navigate(R.id.fragmentSetting)
                true
            }
            R.id.fragmentSearch -> {
                navController.navigate(R.id.fragmentSearch)
                true
            }
            else -> {
                false
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerData(data: PlayerData) {
        when (PlayerManager.service?.isPlaying()) {
            true -> {
                binding.playerLayout.playerButton.setImageDrawable(this.getDrawable(R.drawable.exo_controls_pause))
            }
            false -> {
                binding.playerLayout.playerButton.setImageDrawable(this.getDrawable(R.drawable.exo_controls_play))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 22) {
            AppSingleton().startWorker()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}