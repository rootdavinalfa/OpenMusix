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
import xyz.dvnlabs.openmusix.databinding.FragmentArtistBinding
import xyz.dvnlabs.openmusix.ui.list.ArtistAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel

class FragmentArtist : FragmentHost() {
    private var binding: FragmentArtistBinding? = null
    private val quickVM: QuickListViewModel by sharedViewModel()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentArtistBinding.bind(
            inflater.inflate(
                R.layout.fragment_artist,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ArtistAdapter(R.layout.rv_artist)
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.artistList?.layoutManager = layoutManager
        binding?.artistList?.adapter = adapter
        quickVM.artist.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}