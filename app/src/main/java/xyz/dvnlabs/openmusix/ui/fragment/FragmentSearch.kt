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
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.data.MediaDB
import xyz.dvnlabs.openmusix.databinding.FragmentSearchBinding
import xyz.dvnlabs.openmusix.ui.list.MediaListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import xyz.dvnlabs.openmusix.util.view.AutoGridLayoutManager

class FragmentSearch : FragmentHost() {
    private var binding: FragmentSearchBinding? = null
    private val listVM: ListViewModel by sharedViewModel()
    private val mediaDB: MediaDB by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            FragmentSearchBinding.bind(inflater.inflate(R.layout.fragment_search, container, false))
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MediaListAdapter(R.layout.rv_media)
        val layoutManager = AutoGridLayoutManager(requireContext(), 500)
        binding?.searchList?.layoutManager = layoutManager
        binding?.searchList?.adapter = adapter
        binding?.searchTextInput?.doOnTextChanged { text, _, _, count ->
            if (count == 0) {
                binding?.searchClear?.visibility = View.GONE
            } else {
                binding?.searchClear?.visibility = View.VISIBLE
            }
            lifecycleScope.launch {
                val list = mediaDB.mediaDataDAO().getMedia()
                if (!text.isNullOrEmpty()) {
                    val filtered = list?.filter {
                        "(?i)$text".toRegex().containsMatchIn(it.title)
                    }
                    adapter.setMediaList(filtered!!)
                } else {
                    adapter.setMediaList(emptyList())
                }
            }
        }
        binding?.searchClear?.setOnClickListener {
            binding?.searchTextInput?.text?.clear()
        }
        super.onViewCreated(view, savedInstanceState)
    }
}