/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentAlbumDetailBinding
import xyz.dvnlabs.openmusix.service.OpenMusixAPI
import xyz.dvnlabs.openmusix.ui.list.PListAdapter

class FragmentAlbumDetail : FragmentHost() {
    private var binding: FragmentAlbumDetailBinding? = null
    private val args: FragmentAlbumDetailArgs by navArgs()
    private val mediaDB: MediaDB by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlbumDetailBinding.bind(
            inflater.inflate(
                R.layout.fragment_album_detail,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PListAdapter(R.layout.rv_play_list)
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.albumList?.layoutManager = layoutManager
        binding?.albumList?.adapter = adapter
        lifecycleScope.launch {
            if (args.albumID != -1L) {
                val album = mediaDB.mediaAlbumDAO().getAlbumByID(args.albumID)
                val media = mediaDB.mediaDataDAO().getMediaByAlbum(args.albumID)
                adapter.setMediaList(media)
                binding?.albumName?.text = album.albumName
                binding?.albumArtist?.text = album.artistName
                val imageURL = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    args.albumID
                )


                binding?.albumImage?.let {
                    Glide.with(requireContext())
                        .applyDefaultRequestOptions(
                            RequestOptions()
                                .placeholder(R.drawable.ic_album)
                                .error(R.drawable.ic_album)
                        )
                        .load(imageURL)
                        .transition(
                            DrawableTransitionOptions()
                                .crossFade()
                        ).apply(
                            RequestOptions()
                                .override(600, 600)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                        ).into(it)
                }
            }
        }

        binding?.albumPlay?.setOnClickListener {
            if (args.albumID != -1L) {
                lifecycleScope.launch {
                    val media = mediaDB.mediaDataDAO().getMediaByAlbum(args.albumID)
                    OpenMusixAPI.api?.playNewQueue(medias = media)
                }
            }
        }
        binding?.albumPlay?.setOnLongClickListener {
            Toast.makeText(requireContext(), "Play only this album", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}