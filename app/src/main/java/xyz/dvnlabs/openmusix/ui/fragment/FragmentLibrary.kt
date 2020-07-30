/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.size
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import xyz.dvnlabs.openmusix.R
import xyz.dvnlabs.openmusix.databinding.FragmentLibraryBinding
import xyz.dvnlabs.openmusix.ui.list.MediaListAdapter
import xyz.dvnlabs.openmusix.ui.viewmodel.ListViewModel
import xyz.dvnlabs.openmusix.util.MediaScannerWorker
import xyz.dvnlabs.openmusix.util.view.AutoGridLayoutManager

class FragmentLibrary : FragmentHost() {
    private var binding: FragmentLibraryBinding? = null
    private val listVM: ListViewModel by sharedViewModel()
    private var firstTime = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLibraryBinding.bind(
            inflater.inflate(
                R.layout.fragment_library,
                container,
                false
            )
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = MediaListAdapter(R.layout.rv_media)
        val layoutManager = AutoGridLayoutManager(requireContext(), 500)
        binding?.libraryList?.layoutManager = layoutManager
        binding?.libraryList?.adapter = adapter
        listVM.listMedia.observe(viewLifecycleOwner, Observer { x ->
            adapter.setMediaList(x)
        })

        if (binding?.libraryList?.size != 0) {
            val sharedPref =
                requireContext().getSharedPreferences("current", Context.MODE_PRIVATE)
            val fileID = sharedPref.getLong("file_id", -1)
            val data = listVM.listMedia.value
            val current = data!!.singleOrNull { it.fileID == fileID }
            val index = data.indexOf(current)
            binding?.libraryList?.smoothScrollToPosition(index)
        }

        var checkScrollUp = false
        binding?.libraryList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    if (checkScrollUp) {
                        binding?.libraryHeader?.isExpanded = false
                        checkScrollUp = false
                    }
                } else {
                    if (!checkScrollUp) {
                        binding?.libraryHeader?.isExpanded = true
                        checkScrollUp = true
                    }
                }
            }
        })
        binding?.libraryRefresh?.setOnClickListener {
            Toast.makeText(requireContext(), "Refreshing Libraries", Toast.LENGTH_SHORT).show()
            MediaScannerWorker.setupTaskImmediately(requireContext())
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }
}