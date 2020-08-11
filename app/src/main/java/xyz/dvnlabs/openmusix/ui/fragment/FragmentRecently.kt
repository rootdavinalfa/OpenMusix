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
import xyz.dvnlabs.openmusix.databinding.FragmentRecentlyBinding
import xyz.dvnlabs.openmusix.ui.list.PListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.QuickListViewModel

class FragmentRecently : FragmentHost() {
    private val quickVM: QuickListViewModel by sharedViewModel()
    private var binding: FragmentRecentlyBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecentlyBinding.bind(
            inflater.inflate(
                R.layout.fragment_recently,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PListAdapter(R.layout.rv_play_list)
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.recentAddList?.layoutManager = layoutManager
        binding?.recentAddList?.adapter = adapter
        quickVM.recently.observe(viewLifecycleOwner, Observer {
            adapter.setMediaList(it)
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}