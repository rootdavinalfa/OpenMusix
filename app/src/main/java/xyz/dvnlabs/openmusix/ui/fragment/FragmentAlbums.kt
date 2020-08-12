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
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentAlbumsBinding
import xyz.dvnlabs.openmusix.ui.list.AlbumListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import xyz.dvnlabs.openmusix.util.view.AutoGridLayoutManager

class FragmentAlbums : FragmentHost() {
    private var binding: FragmentAlbumsBinding? = null
    private val mediaDB: MediaDB by inject()
    private val listVM: ListViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlbumsBinding.bind(
            inflater.inflate(
                R.layout.fragment_albums,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var checkScrollUp = false
        binding?.albumsList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    if (checkScrollUp) {
                        binding?.albumsHeader?.isExpanded = false
                        checkScrollUp = false
                    }
                } else {
                    if (!checkScrollUp) {
                        binding?.albumsHeader?.isExpanded = true
                        checkScrollUp = true
                    }
                }
            }
        })

        val adapter = AlbumListAdapter(R.layout.rv_media)
        val layoutManager = AutoGridLayoutManager(requireContext())
        binding?.albumsList?.layoutManager = layoutManager
        binding?.albumsList?.adapter = adapter

        listVM.albumsList.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
    }
}