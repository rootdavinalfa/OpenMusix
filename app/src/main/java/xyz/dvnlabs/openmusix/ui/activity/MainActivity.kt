/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.AnimationUtils
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.app.AppSingleton
import xyz.dvnlabs.openmusix.base.BaseActivity
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.databinding.ActivityMainBinding
import xyz.dvnlabs.openmusix.service.OpenMusixAPI
import xyz.dvnlabs.openmusix.service.PlaybackStatus
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import xyz.dvnlabs.openmusix.util.PermissionData
import xyz.dvnlabs.openmusix.util.PermissionHelper

class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemReselectedListener,
    BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private val listVM: ListViewModel by viewModel()
    private val mediaDB: MediaDB by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val check = arrayOf(
            PermissionData(Manifest.permission.READ_EXTERNAL_STORAGE, 22),
            PermissionData(Manifest.permission.WRITE_EXTERNAL_STORAGE, 22)
        )
        PermissionHelper.checkPermission(this, check)

        val sharedPref = this.getSharedPreferences("current", Context.MODE_PRIVATE)
        var fileID = sharedPref.getLong("file_id", -1)
        val queueID = sharedPref.getLong("queue", -1)
        val currentPosition = sharedPref.getLong("position", -1)

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
                R.id.fragmentLibrary -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                R.id.fragmentAlbums -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                R.id.fragmentAlbumDetail -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                R.id.fragmentGenre -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                R.id.fragmentGenreDetail -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                R.id.fragmentRecently -> {
                    if (fileID != -1L) {
                        binding.playerLayout.playerExpand.isExpanded = true
                    }
                }
                else -> {
                    binding.playerLayout.playerExpand.isExpanded = true
                    binding.playerLayout.bottomNav.menu.findItem(destination.id).isChecked = true
                }
            }
        }

        binding.playerLayout.bottomNav.setOnNavigationItemSelectedListener(this)

        if (fileID == -1L || fileID == 0L) {
            binding.playerLayout.playerExpand.isExpanded = false
        } else {
            lifecycleScope.launchWhenCreated {
                currentPlaying(fileID)
                val media = mediaDB.mediaDataDAO().getMediaByID(fileID)
                if (queueID == -1L && OpenMusixAPI.api?.liveStateChange?.value?.state != PlaybackStatus.PLAYING) {
                    OpenMusixAPI.api?.playDefault(media, currentPosition, false)
                }
                if (queueID != -1L && OpenMusixAPI.api?.liveStateChange?.value?.state != PlaybackStatus.PLAYING) {
                    val queues = mediaDB.mediaQueueDetailDAO().getQueueDetailByQueueID(queueID)
                    val medias = ArrayList<MediaData>()
                    for (i in queues) {
                        val mediax = mediaDB.mediaDataDAO().getMediaByID(i.fileID)
                        medias.add(mediax)
                    }
                    OpenMusixAPI.api?.playNewQueue(medias = medias, playWhenReady = false)
                }
            }
        }

        OpenMusixAPI.api?.liveStateChange?.observe(this, Observer {
            when (it.state) {
                PlaybackStatus.PLAYING -> {
                    binding.playerLayout.playerButton.setImageDrawable(this.getDrawable(R.drawable.exo_controls_pause))
                }
                PlaybackStatus.PAUSED -> {
                    binding.playerLayout.playerButton.setImageDrawable(this.getDrawable(R.drawable.exo_controls_play))
                }
            }
        })

        OpenMusixAPI.api?.liveDataChange?.observe(this, Observer {
            lifecycleScope.launch {
                if (it.currentTag != null) {
                    fileID = it.currentTag as Long
                    currentPlaying(it.currentTag)
                }
            }
        })

        binding.playerLayout.playerButton.setOnClickListener {
            lifecycleScope.launch {
                currentPlaying(fileID)
            }
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.click_anim_1))
            OpenMusixAPI.api?.playPausePlayer()
        }

    }

    private suspend fun currentPlaying(fileID: Long) {

        withContext(Dispatchers.IO) {
            val mediaData = mediaDB.mediaDataDAO().getMedia()

            val current = mediaData.singleOrNull { x ->
                x.fileID == fileID
            }

            val retriever = MediaMetadataRetriever()
            current?.let {
                retriever.setDataSource(this@MainActivity, Uri.parse(it.contentURI))
            }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

            val detail = "$artist - $album"
            val imageURL = current?.albumID?.let {
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    it
                )
            }

            withContext(Dispatchers.Main) {
                binding.playerLayout.playerTitle.text = current?.title ?: ""
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

                Glide.with(this@MainActivity)
                    .applyDefaultRequestOptions(
                        RequestOptions()
                            .placeholder(R.drawable.ic_song)
                            .error(R.drawable.ic_song)
                    )
                    .load(imageURL).transform(BlurTransformation(50, 5))
                    .transition(
                        DrawableTransitionOptions()
                            .crossFade()
                    ).apply(
                        RequestOptions()
                            .override(600, 600)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    ).into(object : CustomTarget<Drawable?>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable?>?
                        ) {
                            binding.playerLayout.playerNavContainer.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            binding.playerLayout.playerNavContainer.background =
                                this@MainActivity.getDrawable(R.color.colorBackgroundAll)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            binding.playerLayout.playerNavContainer.background =
                                this@MainActivity.getDrawable(R.color.colorBackgroundAll)
                        }

                        override fun onLoadStarted(placeholder: Drawable?) {
                            binding.playerLayout.playerNavContainer.background =
                                this@MainActivity.getDrawable(R.color.colorBackgroundAll)
                        }
                    })
            }
        }
    }

    private suspend fun queuePlay() {

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
            R.id.fragmentMenu -> {
                navController.navigate(R.id.fragmentMenu)
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
            R.id.fragmentEqualizer -> {
                navController.navigate(R.id.fragmentEqualizer)
                true
            }
            else -> {
                false
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