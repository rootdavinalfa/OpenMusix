/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.size
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.yarolegovich.discretescrollview.transform.Pivot
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.exo_playback_control_view.view.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentPlayerBinding
import xyz.dvnlabs.openmusix.service.PlayerManager
import xyz.dvnlabs.openmusix.service.event.PlayerChange
import xyz.dvnlabs.openmusix.ui.list.PlayingListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import java.util.concurrent.TimeUnit

class FragmentPlayer : FragmentHost() {
    private var binding: FragmentPlayerBinding? = null
    private val mediaDB: MediaDB by inject()
    private val listVM: ListViewModel by sharedViewModel()
    private var firstTime = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            FragmentPlayerBinding.bind(inflater.inflate(R.layout.fragment_player, container, false))
        val sharedPref = requireContext().getSharedPreferences("current", Context.MODE_PRIVATE)

        Observable.create<Long> { emitter: ObservableEmitter<Long>? ->
            Schedulers.newThread().schedulePeriodicallyDirect({
                val fileID = sharedPref.getLong("file_id", -1)
                emitter?.onNext(fileID)
            }, 0, 500, TimeUnit.MILLISECONDS)
        }.observeOn(AndroidSchedulers.mainThread()).distinctUntilChanged().subscribe {
            listVM.changeFileID(it)
        }
        EventBus.getDefault().register(this)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PlayingListAdapter(R.layout.rv_now_play)
        binding?.playerList?.adapter = adapter
        listVM.listMedia.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
        binding?.playerList?.setItemViewCacheSize(0)
        binding?.playerList?.setItemTransformer(
            ScaleTransformer.Builder()
                .setMaxScale(1.05f)
                .setMinScale(0.8f)
                .setPivotX(Pivot.X.CENTER) // CENTER is a default one
                .setPivotY(Pivot.Y.BOTTOM) // CENTER is a default one
                .build()
        )
        binding?.playerView?.controllerHideOnTouch = false
        binding?.playerView?.showController()
        binding?.playerView?.player = PlayerManager.service?.exoPlayer
        if (PlayerManager.service?.isPlaying() == false) {
            PlayerManager.service?.transportControls?.play()
        }

        listVM.currentFileID.observe(viewLifecycleOwner, Observer {
            lifecycleScope.launch {
                if (binding?.playerList?.size != 0 && it != -1L) {
                    bindData(it)
                }
            }
        })
        binding?.playerList?.onItemMove = { currentPos, oldPos, byUser ->
            if (byUser) {
                val media = listVM.listMedia.value?.get(currentPos)
                val sharedPref =
                    requireContext().getSharedPreferences("current", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    media?.fileID?.let { putLong("file_id", it) }
                    commit()
                }
                firstTime = true
            }

        }

        requireView().seekBar?.onProgressChanged = { progress, byUser ->
            if (byUser) {
                val duration = PlayerManager.service?.exoPlayer?.duration
                val seek = duration?.times(progress.toLong())?.div(100)
                seek?.let {
                    PlayerManager.service?.exoPlayer?.seekTo(it)
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        firstTime = true
        super.onResume()
    }

    private suspend fun bindData(fileID: Long) {
        val mediaData = mediaDB.mediaDataDAO().getMedia()
        val current = mediaData.singleOrNull { x ->
            x.fileID == fileID
        }
        val nowIndex = mediaData.indexOf(current)
        var detail = "Reached End Of Queue"
        if (nowIndex < mediaData.size - 1) {
            val next = mediaData[nowIndex + 1]
            val projection = arrayOf(
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            )
            val selection = "${MediaStore.Audio.AudioColumns._ID} == ${next.fileID}"
            val query = requireContext().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, null
            )
            query.use { x ->
                val albumColumn =
                    query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val artistColumn =
                    query?.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                while (x!!.moveToNext()) {
                    val album = query?.getString(albumColumn!!)
                    val artist = query?.getString(artistColumn!!)
                    detail = "${next.title} - $artist - $album"
                }
            }
        }

        val imageURL = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            current!!.albumID
        )
        requireView().exo_next_song?.text = detail
        requireView().exo_count?.text = "${nowIndex + 1} / ${mediaData.size}"
        attachImage(imageURL)
        if (firstTime) {
            binding?.playerList?.scrollToPosition(nowIndex)
            lifecycleScope.launch {
                val inputStream =
                    requireContext().contentResolver.openInputStream(Uri.parse(current.contentURI))
                requireView().seekBar?.setRawData(inputStream!!.readBytes())
            }

            firstTime = false
        }
    }

    @Subscribe
    fun currentData(data: PlayerChange.CurrentData) {
        val progress = (data.currentPosition.toFloat() / data.currentDuration.toFloat()) * 100
        requireView().seekBar?.progress = progress
    }

    private fun attachImage(imageURL: Uri) {
        Glide.with(requireContext())
            .applyDefaultRequestOptions(
                RequestOptions()
                    .placeholder(R.drawable.ic_song)
                    .error(R.drawable.ic_song)
            )
            .load(imageURL).transform(BlurTransformation(5, 5))
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
                    binding?.playerContainer?.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    binding?.playerContainer?.background = null
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    binding?.playerContainer?.setBackgroundColor(
                        getColor(
                            requireContext(),
                            R.color.colorBackgroundAll
                        )
                    )
                }
            })
    }
}