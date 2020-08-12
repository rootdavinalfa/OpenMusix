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
import xyz.dvnlabs.openmusix.databinding.FragmentArtistDetailBinding
import xyz.dvnlabs.openmusix.ui.list.AlbumListAdapter
import xyz.dvnlabs.openmusix.util.view.AutoGridLayoutManager

class FragmentArtistDetail : FragmentHost() {
    private var binding: FragmentArtistDetailBinding? = null
    private val mediaDB: MediaDB by inject()
    private val args: FragmentArtistDetailArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentArtistDetailBinding.bind(
            inflater.inflate(
                R.layout.fragment_artist_detail,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = AutoGridLayoutManager(requireContext())
        val adapter = AlbumListAdapter(R.layout.rv_media)
        binding?.artistAlbumList?.layoutManager = layoutManager
        binding?.artistAlbumList?.adapter = adapter
        val artist = args.artistID
        lifecycleScope.launch {
            if (artist != -1L) {
                val artistM = mediaDB.mediaArtistDAO().getArtistByID(args.artistID)
                val album = mediaDB.mediaAlbumDAO().getAlbumByArtist(artistM.artistName)
                adapter.setMediaList(album)
                binding?.artistName?.text = artistM.artistName
                binding?.artistTrack?.text = artistM.trackCount.toString()
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}