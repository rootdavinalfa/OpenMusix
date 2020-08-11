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
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentGenreDetailBinding
import xyz.dvnlabs.openmusix.service.OpenMusixAPI
import xyz.dvnlabs.openmusix.ui.list.PListAdapter

class FragmentGenreDetail : FragmentHost() {
    private var binding: FragmentGenreDetailBinding? = null
    private val args: FragmentGenreDetailArgs by navArgs()
    private val mediaDB: MediaDB by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGenreDetailBinding.bind(
            inflater.inflate(
                R.layout.fragment_genre_detail,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PListAdapter(R.layout.rv_play_list)
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.genreList?.layoutManager = layoutManager
        binding?.genreList?.adapter = adapter
        lifecycleScope.launch {
            if (args.genreID != -1L) {
                val genre = mediaDB.mediaGenreDAO().getGenre(args.genreID)
                var name = genre?.genreName
                if (name?.isBlank()!!) {
                    name = "Unknown Genre"
                }
                binding?.genreName?.text = name
                val mediaRaw = mediaDB.mediaDataDAO().getMediaDataByGenre(args.genreID)
                /*val media = ArrayList<MediaData>()
                for (i in mediaRaw) {
                    media.add(i.data)
                }*/
                adapter.setMediaList(mediaRaw)
            }
        }

        binding?.genrePlay?.setOnClickListener {
            if (args.genreID != -1L) {
                lifecycleScope.launch {
                    val mediaRaw = mediaDB.mediaDataDAO().getMediaDataByGenre(args.genreID)
                    /*val media = ArrayList<MediaData>()
                    for (i in mediaRaw) {
                        media.add(i.data)
                    }*/
                    OpenMusixAPI.api?.playNewQueue(medias = mediaRaw)
                }
            }
        }
        binding?.genrePlay?.setOnLongClickListener {
            Toast.makeText(requireContext(), "Play only this genre", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}