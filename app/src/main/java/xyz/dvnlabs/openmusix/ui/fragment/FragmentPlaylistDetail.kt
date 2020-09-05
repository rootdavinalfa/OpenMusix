/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentPlaylistDetailBinding
import xyz.dvnlabs.openmusix.service.OpenMusixAPI
import xyz.dvnlabs.openmusix.ui.list.MediaListAdapter
import xyz.dvnlabs.openmusix.util.Converter
import xyz.dvnlabs.openmusix.util.view.AutoGridLayoutManager

class FragmentPlaylistDetail : FragmentHost() {
    var binding: FragmentPlaylistDetailBinding? = null
    private val args: FragmentPlaylistDetailArgs by navArgs()
    private val mediaDB: MediaDB by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaylistDetailBinding.bind(
            inflater.inflate(
                R.layout.fragment_playlist_detail,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MediaListAdapter(R.layout.rv_media)
        binding?.playlistSongList?.layoutManager = AutoGridLayoutManager(requireContext())
        binding?.playlistSongList?.adapter = adapter
        if (args.queueID != -1L) {
            lifecycleScope.launch {
                val data = mediaDB.mediaQueueDAO().getQueueByUID(args.queueID)
                binding?.playlistDetailText?.text = data.name

                val queueDetail =
                    mediaDB.mediaQueueDetailDAO().getQueueDetailByQueueID(args.queueID)

                val mediaList = queueDetail.map { qd ->
                    mediaDB.mediaDataDAO().getMediaByID(qd.fileID)
                }
                adapter.setMediaList(mediaList)
                binding?.playlistPlay?.setOnClickListener {
                    lifecycleScope.launch {
                        val mediaData = mediaDB.mediaDataDAO().getMediaByQueue(args.queueID)
                        val queue = Converter().convertMediaDataToQueue(mediaData)
                        if (queue != null) {
                            OpenMusixAPI.api?.playNewQueue(
                                id = args.queueID,
                                medias = mediaData,
                                sys = false
                            )
                        }
                    }
                }
            }
        }

    }
}