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
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.databinding.FragmentMenuBinding
import xyz.dvnlabs.openmusix.ui.list.APlaylistAdapter
import xyz.dvnlabs.openmusix.ui.list.MediaListAdapter
import xyz.dvnlabs.openmusix.ui.list.PListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel

class FragmentMenu : FragmentHost() {
    private var binding: FragmentMenuBinding? = null
    private val quickVM: QuickListViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuBinding.bind(
            inflater.inflate(
                R.layout.fragment_menu,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = view.findNavController()
        binding?.menuAllSong?.setOnClickListener {
            navController.navigate(R.id.fragmentLibrary)
        }
        binding?.menuAlbum?.setOnClickListener {
            navController.navigate(R.id.fragmentAlbums)
        }
        binding?.menuGenre?.setOnClickListener {
            navController.navigate(R.id.fragmentGenre)
        }
        binding?.menuArtist?.setOnClickListener {
            navController.navigate(R.id.fragmentArtist)
        }

        binding?.menuRecentlyAdded?.setOnClickListener {
            navController.navigate(R.id.fragmentRecently)
        }
        binding?.menuPlaylist?.setOnClickListener {
            navController.navigate(R.id.fragmentPlaylist)
        }
        val adapter = MediaListAdapter(R.layout.rv_media)
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.menuRecentlyAddedList?.layoutManager = layoutManager
        binding?.menuRecentlyAddedList?.adapter = adapter
        binding?.menuRecentlyAddedList?.setItemViewCacheSize(20)
        quickVM.recentPlay.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
        val adapterTop = PListAdapter(R.layout.rv_play_list)
        val lManager = LinearLayoutManager(requireContext())
        binding?.menuTopList?.layoutManager = lManager
        binding?.menuTopList?.adapter = adapterTop
        quickVM.topPlay.observe(viewLifecycleOwner, Observer {
            adapterTop.setMediaList(it)
        })

        val playlistAdapter = APlaylistAdapter(R.layout.rv_playlist)
        val lManagerPlist = LinearLayoutManager(requireContext())
        binding?.menuPlaylistList?.layoutManager = lManagerPlist
        binding?.menuPlaylistList?.adapter = playlistAdapter
        quickVM.playlist.observe(viewLifecycleOwner, Observer {
            playlistAdapter.setMediaList(it)
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}