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
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.databinding.FragmentPlaylistBinding
import xyz.dvnlabs.openmusix.ui.list.APlaylistAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel

class FragmentPlaylist : FragmentHost() {
    var binding: FragmentPlaylistBinding? = null
    private val quickVM: QuickListViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaylistBinding.bind(
            inflater.inflate(
                R.layout.fragment_playlist,
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
        val adapter = APlaylistAdapter(R.layout.rv_playlist)
        binding?.playlistList?.layoutManager = LinearLayoutManager(requireContext())
        binding?.playlistList?.adapter = adapter
        quickVM.playlist.observe(viewLifecycleOwner, Observer {
            binding?.errorText?.visibility = if (it.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            adapter.setMediaList(it)
        })

    }
}