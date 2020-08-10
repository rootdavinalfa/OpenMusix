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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentGenreBinding
import xyz.dvnlabs.openmusix.ui.list.GenreMetaAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel

class FragmentGenre : FragmentHost() {
    private var binding: FragmentGenreBinding? = null
    private val mediaDB: MediaDB by inject()
    private val quickVM: QuickListViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGenreBinding.bind(
            inflater.inflate(
                R.layout.fragment_genre,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = GenreMetaAdapter(R.layout.rv_genre_selector)
        val lManager = LinearLayoutManager(requireContext())
        quickVM.genre.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
        binding?.genreList?.layoutManager = lManager
        binding?.genreList?.adapter = adapter
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}